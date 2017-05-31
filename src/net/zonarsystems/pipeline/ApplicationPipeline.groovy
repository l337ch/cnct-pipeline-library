package net.zonarsystems.pipeline
import java.util.regex.Pattern

class ApplicationPipeline implements Serializable {
  def steps
  def application
  
  def script
  def getEnvironment() { return script.env }
  def getFileLoader() { return script.fileLoader }

  def settings

  def pipeline
  def getPipeline() { bailOnUninitialized(); pipeline[application] }
  def getPipeline(app) { bailOnUninitialized(); pipeline[app] }

  def helpers
  def e2e
  def overrides
  
  def ready = false
  def uniqueJenkinsId = ''

  final STAGING_TAG = "staging"
  final PROD_TAG = "prod"

  def bailOnUninitialized() { if (!this.ready) { throw new Exception('Pipeline not initialized, run init() first') } }

  ApplicationPipeline(steps, application, script, overrides = [:], e2e = [:]) {
    this.steps = steps
    this.application = application
    this.script = script
    this.e2e = e2e
    this.overrides = overrides
  }

  @NonCPS
  def isJobStartedByTimer(build) {
    def buildCauses = build.rawBuild.getCauses()
    for ( buildCause in buildCauses ) {
      if (buildCause != null) {
        def causeDescription = buildCause.getShortDescription()
        if (causeDescription.contains("Started by timer")) {
          return true
        }
      }
    }
    return false
  }

  def pipelineCheckout() {
    bailOnUninitialized()

    getSteps().stage ('Checkout') {
      if (getEnvironment().CHANGE_ID != null) {
        getHelpers().githubPRCheckout(application, getEnvironment().CHANGE_ID)
      } else {
        if (getEnvironment().BRANCH_NAME != 'master') {
          getSteps().error 'Aborting non-master branch build'
        }

        getHelpers().githubBranchCheckout(application, getEnvironment().BRANCH_NAME)
      }
    }
  }

  def chartMake(command) {
    def makefileText = getSteps().libraryResource 'chart.makefile'

    getSteps().writeFile(file: "charts/Makefile", text: makefileText)

    try {
      getSteps().sh "make ${command} CHART_REPO=${getSettings().chartRepo} REPO_BUCKET=${getSettings().chartBucket}"
    } finally {
      getSteps().sh 'rm -f charts/Makefile'
    }
  }

  def upgradeHelmCharts(directory, dockerImagesTag) {
    bailOnUninitialized()

    getSteps().stage ('Deploy Helm chart(s)') {
      def chartName = getHelmChartName(directory)
      def upgradeString = "helm upgrade ${getPipeline().helm} ${getSettings().githubOrg}/${chartName} --version ${getHelmChartVersion(directory)} --install --namespace prod"
      
      def prodOverrides = getChartOverrides(getOverrides(), chartName, 'prod')
      if (prodOverrides) {
        upgradeString += " --set ${prodOverrides}"
      }

      // add repo
      getSteps().sh "helm repo add ${getSettings().githubOrg} ${getSettings().chartRepo}"
      // update dependencies
      getSteps().sh 'helm repo update'
      getSteps().retry(getSettings().maxRetry) {
        getSteps().sh upgradeString
      }
    }
  }

  def initKubeAndHelm() {
    bailOnUninitialized()

    getSteps().stage ('Init kubectl and helm') {
      getSteps().sh """
  gcloud container clusters get-credentials ${getEnvironment().GKE_CLUSTER_NAME} --zone ${getEnvironment().GKE_PRIMARY_ZONE} --project ${getEnvironment().GKE_PROJECT_NAME}
  kubectl cluster-info
  helm init
  """
    }
  }

  def buildContainersWithTag(imageTag) {
    bailOnUninitialized()

    getSteps().stage ('Build docker containers') {
      // if there is a docker file directly under rootfs - build it 
      // and name the GCR repo after the pipeline name
      // otherwise
      // enumerate all the folders under rootfs and build Dockerfiles in each one
      // name the GCR repo after foldername
      def dockerfileFolders = getScript().listFolders('./rootfs')
      for (def i = 0; i < dockerfileFolders.size(); i++) {
        if (getSteps().fileExists("${dockerfileFolders[i]}/Dockerfile")) {
          def imageName = dockerfileFolders[i].split('/').last()

          if (getSteps().fileExists("${dockerfileFolders[i]}/Makefile")) {
            getSteps().sh "make -C ${dockerfileFolders[i]}"
          }

          def useArtifactory = getSteps().sh(returnStatus: true, script: "grep \"ARG ARTIFACTORY_IP=\" ${dockerfileFolders[i]}/Dockerfile")
          if (useArtifactory == 0) {
            getSteps().sh "gcloud docker -- build -t ${getSettings().dockerRegistry}/${imageName}:${imageTag} --build-arg ARTIFACTORY_IP=${getScript().getHostIp(getSettings().artifactory)} ${dockerfileFolders[i]}"
          } else {
            getSteps().sh "gcloud docker -- build -t ${getSettings().dockerRegistry}/${imageName}:${imageTag} ${dockerfileFolders[i]}"
          }
        }
      }
    }
  }

  def pushContainersWithTag(imageTag) {
    bailOnUninitialized()

    getSteps().stage ('Push docker containers') {
      // if there is a docker file directly under rootfs - build it 
      // and name the GCR repo after the pipeline name
      // otherwise
      // enumerate all the folders under rootfs and build Dockerfiles in each one
      // name the GCR repo after foldername
      def dockerfileFolders = getScript().listFolders('./rootfs')
      for (def i = 0; i < dockerfileFolders.size(); i++) {
        if (getSteps().fileExists("${dockerfileFolders[i]}/Dockerfile")) {
          def imageName = dockerfileFolders[i].split('/').last()
          getSteps().sh "gcloud docker -- push ${getSettings().dockerRegistry}/${imageName}:${imageTag}"
        }
      }
    }
  }

  def tagContainers(currentTag, newTag) {
    bailOnUninitialized()

    getSteps().stage ('Push docker containers') {
      // if there is a docker file directly under rootfs - build it 
      // and name the GCR repo after the pipeline name
      // otherwise
      // enumerate all the folders under rootfs and build Dockerfiles in each one
      // name the GCR repo after foldername
      def dockerfileFolders = getScript().listFolders('./rootfs')
      for (def i = 0; i < dockerfileFolders.size(); i++) {
        if (getSteps().fileExists("${dockerfileFolders[i]}/Dockerfile")) {
          def imageName = dockerfileFolders[i].split('/').last()
          getSteps().sh "gcloud docker -- tag ${getSettings().dockerRegistry}/${imageName}:${currentTag} ${getSettings().dockerRegistry}/${imageName}:${newTag}"
        }
      }
    }
  }
  
  def checkImageForNewPackageVersion(dockerImage, packageName) {
    //bailOnUninitialized()
    getSteps().echo ("Checking for newer version of ${packageName} in image ${dockerImage}")
    //use yum to check the installed and available version
    getSteps().echo "gcloud docker -- run -it ${dockerImage} yum list installed ${packageName} | awk \'END {print \$2 }\'"
    
    //def availableVersion = getSteps().sh(script: "gcloud docker -- run -it ${dockerImage} yum list available ${packageName} | awk \'END {print \$2 }\'", returnStdout: true)
    //getSteps().echo "${packageName} is at ${currentVersion} latest=${availableVersion}"
    //if (currentVersion != availableVersion) {
    //   getSteps().echo "${packageName} has newer version available: ${availableVersion}"
    //   return true
    //}
      
    return false
  }

  def isNewZonarReleaseAvailable() {
    def isNewRelease = false;
    getSteps().stage('Checking if Zonar has released new artifacts for this chart'){
      
      def chartsFolders=getScript().listFolders('./charts')
      for(def i=0;i<chartsFolders.size();i++){
        
        chartPackageCheck:
        getSteps().echo "checking for new packages in chart: ${chartsFolders[i]}"
        if(getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")){
          
          def chartImages=getHelmChartValue(chartsFolders[i],"images")
          def zonarPackages=getHelmChartValue(chartsFolders[i],"zonar_apps")
          getSteps().echo "chart images: ${chartImages}"
          getSteps().echo "zonar packages: ${zonarPackages}"
          for(image in chartImages){
            getSteps().sh "ls"
            if(image.key !="pullPolicy"){
              getSteps().echo "found image ${image.key}:  ${image.value}"
              def imagePackages=zonarPackages.get(image.key)
              for(zonarPackage in imagePackages) {
                getSteps().echo "checking package ${zonarPackage.key}"
                
                if(checkImageForNewPackageVersion(image.value,zonarPackage.key)){
                  isNewRelease = true
                  break chartPackageCheck
                }
              }
            }

          }
        }
      }
      return isNewRelease;
    }
  }

  /**
   * Returns the zonar application versions as a helm chart overridable list
   * 
   * @return a comma deliminated list of chart version proiperties
   */
  def getZonarAppVersionOverrides() {
    getSteps().stage('Getting Zonar package versions'){
      def chartsFolders=getScript().listFolders('./charts')
      def packageVersions=[:]
      for(def i=0;i<chartsFolders.size();i++){
        if(getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")){
          def chartImages=getHelmChartValue(chartsFolders[i],"images")
          def zonarPackages=getHelmChartValue(chartsFolders[i],"zonar_apps")

          for(image in chartImages){
            if(image.key!="pullPolicy"){
              def imagePackages=zonarPackages.get(image.key);
              for(zonarPackage in imagePackages){
                def appVersion=getSteps().sh "docker run -it ${image.value} -- yum list installed ${zonarPackage.key} | awk 'END {print \$2 }'"
                packageVersions["zonar_apps.${image.key}.${zonarPackage.key}"]=appVersion
              }
            }

          }
        }
      }
      return packageVersions
    }
  }

  def lintHelmCharts() {
    bailOnUninitialized()

    if (getSteps().fileExists('./charts')) {
      getSteps().stage ('Lint Helm chart(s)') {
        chartMake('lint -C charts')
      }
    }
  }

  def uploadChartsToRepo() {
    bailOnUninitialized()
    if (getSteps().fileExists('./charts')) {
      getSteps().stage ('Upload Helm chart(s) to helm repo') {
        getSteps().retry(getSettings().maxRetry) {
          getSteps().sh "gcloud auth activate-service-account ${getEnvironment().HELM_GKE_SERVICE_ACCOUNT} --key-file /etc/helm/service-account.json"
          chartMake('all -C charts')
          getSteps().sh "gcloud auth activate-service-account ${getEnvironment().MAIN_GKE_SERVICE_ACCOUNT} --key-file /etc/gke/service-account.json"
        }
      }
    }
  }

  def deployHelmChartsFromPath(path, namespace, releaseName) {
    bailOnUninitialized()

    getSteps().stage ("Deploy Helm chart(s) to ${namespace} namespace") {
      def chartName = getHelmChartName(path)

      // add repo (for requirements yaml charts) and pull dependencies
      getSteps().sh "helm repo add ${getSettings().githubOrg} ${getSettings().chartRepo}"
      getSteps().sh "helm dependency update ${path}"

      def commandString = "helm install ${path} --name ${releaseName} --namespace ${namespace} --wait" 
      def testOverrides = getChartOverrides(getOverrides(), chartName, 'staging')
      if (testOverrides) {
        commandString += " --set ${testOverrides}"
      }

      try {
        getSteps().sh "${commandString}"
      } catch (e) {
        deleteHelmRelease(releaseName)
        throw e
      }
    }
  }

  def e2eTestHelmCharts(namespace, releaseName) {
    bailOnUninitialized()

    getSteps().stage ('Helm chart(s) end to end testing') {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        if (getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")) {
          deployHelmChartsFromPath(
            chartsFolders[i],
            'staging',  
            releaseName
          )

          try {
            if (getSteps().fileExists('./test/e2e')) {
              // transform test dictionary into '--key=value' format
              def e2eVars = getScript().getE2eVars {
                e2e = getE2e()
              }

              getSteps().sh("ginkgo ./test/e2e/ -- ${e2eVars}")
            }
          } finally {
            if (getSteps().fileExists('./test/e2e/junit_1.xml')) {
              getSteps().junit('test/e2e/junit_*.xml')
            }
            deleteHelmRelease(releaseName)
          }
        }
      }
    }
  }
  
  def smokeTestHelmCharts(namespace, releaseName) {
    bailOnUninitialized()

    getSteps().stage ('Determine if smoke testing necessary') {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        if (getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")) {
          if (getPipeline().smoketest) {  
            def chartName = getHelmChartName(chartsFolders[i])
            try {
              deployHelmChartsFromPath(
                chartsFolders[i],
                'staging',
                releaseName
              )
              getSteps().stage ("execute smoke tests") {
                getSteps().retry(getSettings().maxRetry) {
                  def testStdout = getSteps().sh(returnStdout: true, script: "helm test ${releaseName} --cleanup")
                  if (testStdout ==~ /(?s).*FAILED\:.*/) {
                    getSteps().error "Smoke test error: ${testStdout}"
                  } 
                }
              }
            } finally {
              deleteHelmRelease(releaseName)
            }
          }
        }
      }
    }
  }

  def deleteHelmRelease(releaseName) {
    bailOnUninitialized()
    getSteps().stage ('Delete helm release') {
      getSteps().sh "helm delete --purge ${releaseName} || true"
    }
  }

  def getHelmChartName(directory) {
      bailOnUninitialized()
      def chartYaml = getScript().parseYaml {
         yaml = getSteps().readFile("${directory}/Chart.yaml")
      }
      return chartYaml.name
  }
  
  def getHelmChartVersion(directory) {
    bailOnUninitialized()

    def chartYaml = getScript().parseYaml {
      yaml = getSteps().readFile("${directory}/Chart.yaml")
    }

    return chartYaml.version
  }
  
  def getHelmChartValue(directory, value) {
    bailOnUninitialized()

    def chartYaml = getScript().parseYaml {
      yaml = getSteps().readFile("${directory}/values.yaml")
    }

    return chartYaml[value]
  }

  def updateChartVersionMetadata(sha) {
    bailOnUninitialized()

    getSteps().stage ('Increment chart(s) version(s)') {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {

        // read in Chart.yaml
        def chartYaml = getScript().parseYaml {
          yaml = getSteps().readFile("${chartsFolders[i]}/Chart.yaml")
        }
        
        def verComponents = []
        verComponents.addAll(chartYaml.version.toString().split(Pattern.quote('+')))
        
        if (verComponents.size() > 1) {
          verComponents[1] = sha
        } else {
          verComponents << sha
        }

        chartYaml.version = verComponents.join('+')

        // dump YAML back
        def changedYAML = getScript().toYaml {
          obj = chartYaml
        }

        // write to file
        getSteps().writeFile(file: "${chartsFolders[i]}/Chart.yaml", text: changedYAML)
      }
    }
  }

  def setDefaultValues(useTag) {
    bailOnUninitialized()

    getSteps().stage ('Inject new docker tags into values.yaml') {

      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        def reqYaml = getScript().parseYaml {
          yaml = getSteps().readFile("${chartsFolders[i]}/values.yaml")
        }

        def dockerfileFolders = getScript().listFolders('./rootfs')
        for (def k = 0; k < dockerfileFolders.size(); k++) {
          def imageName = dockerfileFolders[k].split('/').last()

          if (reqYaml.images && reqYaml.images[imageName]) {
            // toString() is required, otherwise snakeyaml will serialize this into something that is not a string.
            reqYaml.images[imageName] = "${getSettings().dockerRegistry}/${imageName}:${useTag}".toString()
          }
        }

        // dump YAML back
        def changedYAML = getScript().toYaml {
          obj = reqYaml
        }

        // write to file
        getSteps().writeFile(file: "${chartsFolders[i]}/values.yaml", text: changedYAML)
      }
    }
  }

  // init things that need node context
  def init() {
    getSteps().podTemplate(
      label: "env-${application}", 
      containers: [getSteps().containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave:2.62-alpine', args: '${computer.jnlpmac} ${computer.name}'),], 
      volumes: []) {
      getSteps().node ("env-${application}") {
        this.settings = getFileLoader().fromGit(
          'settings', 
          'https://github.com/samsung-cnct/zonar-pipeline.git', 
          'master', 
          'repo-scan-access', 
          ''
        ).getConfig()

        def pipelineConfig = getFileLoader().fromGit(
          'pipeline', 
          "https://github.com/samsung-cnct/zonar-pipeline.git", 
          'master', 
          "repo-scan-access", 
          ''
        ).getConfig()

        this.pipeline = pipelineConfig
        this.helpers = new PipelineHelpers(this.steps, this.settings, pipelineConfig)
        this.uniqueJenkinsId = getEnvironment().UNIQUE_JENKINS_ID
      }
    }

    // set ready
    ready = true
  }

  @NonCPS
  def getChartOverrides(allOverrides, chart, type) {
    def res = null

    if (allOverrides) {
      def chartOverrides = allOverrides.get(chart) 
      if (chartOverrides) {
        def chartTypeOverrides = chartOverrides.get(type)
        if (chartTypeOverrides) {
          res = chartTypeOverrides.inject([]) { result, entry ->
            result << "${entry.key}=${entry.value.toString()}"
          }.join(',')
        }
      }

      def zonarVersionOverrides=getZonarAppVersionOverrides()
      if(zonarVersionOverrides){res=res+zonarVersionOverrides.inject([]){ result,entry->
          result<<"${entry.key}=${entry.value.toString()}"}.join(',')
      }

    }

    return res 
  }

  def pipelineRun() {
    bailOnUninitialized();

    // no concurrent master or PR builds, as charts use cluster-unique resources.
    getSteps().properties(
      [
        getSteps().disableConcurrentBuilds(),
        getSteps().pipelineTriggers([getSteps().cron(getSettings().crontab)])
      ]
    )

    getSteps().podTemplate(label: "CI-${application}", containers: [
      getSteps().containerTemplate(
        name: 'jnlp', 
        image: 'jenkinsci/jnlp-slave:2.62-alpine', 
        args: '${computer.jnlpmac} ${computer.name}'),
      getSteps().containerTemplate(
        name: 'gke', 
        image: "${getSettings().dockerRegistry}/jenkins-gke:latest", 
        ttyEnabled: true, 
        command: 'cat', 
        alwaysPullImage: true),
    ],
    volumes: [
      getSteps().hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
      getSteps().secretVolume(mountPath: '/etc/gke', secretName: "jenkins-gke-${uniqueJenkinsId}"),
      getSteps().secretVolume(mountPath: '/etc/helm', secretName: "jenkins-helm-${uniqueJenkinsId}")
    ]) {
      
      getSteps().node ("CI-${application}") {
        def err = null
        
        // default message
        def notifyMessage = 'Build succeeded for ' + "${getEnvironment().JOB_NAME} number ${getEnvironment().BUILD_NUMBER} (${getEnvironment().BUILD_URL})"
        def notifyColor = 'good'
 
        try {

          // Checkout source code, from PR or master
          pipelineCheckout()
          
      
          // Inside jenkins-gke tool container
          getSteps().container('gke') {
            
            def isNewReleaseAvailable = false 
            
            if(isJobStartedByTimer(getScript().currentBuild)) {
                isNewReleaseAvailable = isNewZonarReleaseAvailable()
                // check for new zonar release
                if(!isNewReleaseAvailable){
                  getSteps().echo "No new packages available - stopping execution"
                  getSteps().stage('Notify'){
                    notifyMessage = 'No new packages available - exiting timer build'
                    getHelpers().sendSlack(
                        getPipeline().slack,notifyMessage,notifyColor)
                  }
                  getScript().currentBuild.result = 'SUCCESS'
                  getSteps().sh "exit 0"
                  return;
                } else {
                  getSteps().echo "New package(s) available - executing pipeline"
                }
            }
             
            // pull kubeconfig from GKE and init helm
            initKubeAndHelm()

            // build docker containers and push them with current SHA tag and 'latest'
            buildContainersWithTag(getScript().getGitSha())
            pushContainersWithTag(getScript().getGitSha())
            tagContainers(getScript().getGitSha(), 'latest')
            pushContainersWithTag('latest')
            
            // update chart semver metadata
            updateChartVersionMetadata(getScript().getGitSha())

            // inject new tag into chart values.yaml
            setDefaultValues(getScript().getGitSha())

            // if this is a Pull Request change
            if (getEnvironment().CHANGE_ID || isNewReleaseAvailable ) {

              // tag and push containers with staging tag
              tagContainers(getScript().getGitSha(), STAGING_TAG)
              pushContainersWithTag(STAGING_TAG)

              // lint, and deploy charts to staging namespace 
              // with overriden 
              // and injected test values 
              // without uploading to helm repo
              lintHelmCharts()

              // lock on helm release name - this way we can avoid 
              // port name collisions between concurrently running PR jobs
              // test the deployed charts, destroy the deployments
              if (getEnvironment().CHANGE_ID) {
                smokeTestHelmCharts(
                  'staging', 
                  "${getPipeline().helm}-${getEnvironment().CHANGE_ID}-${getEnvironment().BUILD_NUMBER}"
                )
              } else {
                smokeTestHelmCharts(
                  'staging',
                  "${getPipeline().helm}-master-${getEnvironment().BUILD_NUMBER}"
                )
              }

              getSteps().lock(getPipeline().helm) {
                if (getPipeline().deploy) {
                  e2eTestHelmCharts(
                    'staging', 
                    "${getPipeline().helm}-e2e"
                  )
                }
              }
            } else {
              // tag and push containers with staging tag
              tagContainers(getScript().getGitSha(), PROD_TAG)
              pushContainersWithTag(PROD_TAG)

              // package and upload charts to helm repo
              uploadChartsToRepo()

              // if pipeline component is marked deployable,
              // deploy it.
              if (getPipeline().deploy) {
                def chartsFolders = getScript().listFolders('./charts')
                for (def i = 0; i < chartsFolders.size(); i++) {
                  if (getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")) {                    
                    upgradeHelmCharts(
                      chartsFolders[i], 
                      getScript().getGitSha()
                    )
                  }
                }
              }
            }
          }
        } catch (e) {
          getScript().currentBuild.result = 'FAILURE'
          notifyMessage = 'Build failed for ' + "${getEnvironment().JOB_NAME} number ${getEnvironment().BUILD_NUMBER} (${getEnvironment().BUILD_URL}) : ${e.getMessage()}"
          notifyColor = 'danger'
          err = e
        } finally {
          getSteps().stage ('Notify') {
            getHelpers().sendSlack(
              getPipeline().slack,
              notifyMessage,
              notifyColor)
          }

          if (err) {
            throw err
          }
        }
      }
    }
  }
}