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
  
  def ready = false
  def uniqueJenkinsId = ''

  def bailOnUninitialized() { if (!this.ready) { throw new Exception('Pipeline not initialized, run init() first') } }

  ApplicationPipeline(steps, application, script) {
    this.steps = steps
    this.application = application
    this.script = script
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

  def upgradeHelmCharts(dockerImagesTag, overrides) {
    bailOnUninitialized()

    getSteps().stage ('Deploy Helm chart(s)') {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        def upgradeString = "helm upgrade --install ${getPipeline().helm} ${getSettings().githubOrg}/${chartName} --version ${getHelmChartVersion(chartName)}"
        if (overrides) {
          upgradeString += " --set ${overrides}"
        }
        getSteps().echo """TODO:
  helm repo add ${getSettings().githubOrg} ${getSettings().chartRepo}
  ${upgradeString}
  """
      }
    }
  }

  def initKubeAndHelm() {
    bailOnUninitialized()

    getSteps().stage ('Init kubectl and helm') {
      getSteps().sh """
  gcloud container clusters get-credentials ${getEnvironment().GKE_CLUSTER_NAME} --zone ${getEnvironment().GKE_PRIMARY_ZONE} --project ${getEnvironment().GKE_PROJECT_NAME}
  helm init
  """
    }
  }

  def buildAndPushContainersWithTag(imageTag) {
    bailOnUninitialized()

    getSteps().stage ('Build and push docker containers') {
      // if there is a docker file directly under rootfs - build it 
      // and name the GCR repo after the pipeline name
      // otherwise
      // enumerate all the folders under rootfs and build Dockerfiles in each one
      // name the GCR repo after foldername
      def dockerfileFolders = getScript().listFolders('./rootfs')
      for (def i = 0; i < dockerfileFolders.size(); i++) {
        if (getSteps().fileExists("${dockerfileFolders[i]}/Dockerfile")) {
          def imageName = dockerfileFolders[i].split('/').last()
          getSteps().sh "gcloud docker -- build -t ${getSettings().dockerRegistry}/${imageName}:${imageTag} --build-arg ARTIFACTORY_IP=${getScript().getHostIp(getSettings().artifactory)} ${dockerfileFolders[i]}"
          getSteps().sh "gcloud docker -- push ${getSettings().dockerRegistry}/${imageName}:${imageTag}"
        }
      }
    }
  }

  def lintHelmCharts() {
    bailOnUninitialized()

    getSteps().stage ('Lint Helm chart(s)') {
      getSteps().sh 'make lint -C charts'
    }
  }

  def uploadChartsToRepo() {
    bailOnUninitialized()

    getSteps().stage ('Upload Helm chart(s) to helm repo') {
      getSteps().sh """
  gcloud auth activate-service-account ${getEnvironment().HELM_GKE_SERVICE_ACCOUNT} --key-file /etc/helm/helm-service-account.json
  make all -C charts
  gcloud auth activate-service-account ${getEnvironment().MAIN_GKE_SERVICE_ACCOUNT} --key-file /etc/gke/service-account.json
  """
    }
  }

  def deployHelmChartsFromPath(namespace, releaseName, testOverrides) {
    bailOnUninitialized()

    getSteps().stage ("Deploy Helm chart(s) to ${namespace} namespace") {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        def commandString = "helm install ${chartsFolders[i]} --name ${releaseName} --namespace ${namespace}" 
        if (testOverrides) {
          commandString += " --set ${testOverrides}"
        }

        getSteps().sh "${commandString}"
      }
    }
  }

  def testHelmCharts(namespace, releaseName) {
    bailOnUninitialized()

    getSteps().stage ('Test Helm chart(s)') {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        if (getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")) {
          getSteps().echo "TODO: test ${chartsFolders[i]} deployed to ${namespace} namespace"
          getSteps().sh "helm delete --purge ${releaseName}"
        }
      }
    }
  }

  def getHelmChartVersion(chartName) {
    bailOnUninitialized()

    def chartYaml = getScript().parseYaml {
      yaml = getSteps().readTrusted("charts/${chartName}/Chart.yaml")
    }

    return chartYaml.version
  }

  def updateChartVersionMetadata(sha) {
    bailOnUninitialized()

    getSteps().stage ('Increment chart(s) version(s)') {
      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {

        // read in Chart.yaml
        def chartYaml = getScript().parseYaml {
          yaml = getSteps().readTrusted("${chartsFolders[i]}/Chart.yaml")
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

  def setDefaultValues(gitSha) {
    bailOnUninitialized()

    getSteps().stage ('Inject new docker tags into values.yaml') {

      def chartsFolders = getScript().listFolders('./charts')
      for (def i = 0; i < chartsFolders.size(); i++) {
        def reqYaml = getScript().parseYaml {
          yaml = getSteps().readTrusted("${chartsFolders[i]}/values.yaml")
        }

        def dockerfileFolders = getScript().listFolders('./rootfs')
        for (def k = 0; k < dockerfileFolders.size(); k++) {
          def imageName = dockerfileFolders[k].split('/').last()

          if (reqYaml.images && reqYaml.images[imageName]) {
            // toString() is required, otherwise snakeyaml will serialize this into something that is not a string.
            reqYaml.images[imageName] = "${getSettings().dockerRegistry}/${imageName}:${gitSha}".toString()
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

  def pushChangesToGithub() {
    bailOnUninitialized()

    getSteps().stage ('Push changes to github if needed') {
      getSteps().withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: getSettings().githubScanCredentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
        getSteps().sh("git config user.name \"${getSettings().githubAdmin}\"")
        getSteps().sh("git config user.email \"${getSettings().githubAdmin}@zonarsystems.net\"")
        getSteps().sh("git checkout master")
        getSteps().sh("git add .")
        getSteps().sh("git commit -m \"${getEnvironment().JOB_NAME} number ${getEnvironment().BUILD_NUMBER} (${getEnvironment().BUILD_URL})\"")
        getSteps().sh("git push https://${getEnvironment().GIT_USERNAME}:${getEnvironment().GIT_PASSWORD}@github.com/${getSettings().githubOrg}/${getPipeline().repo} --all")
      }
    }
  }

  // init things that need node context
  def init() {
    getSteps().podTemplate(label: "env-${application}", containers: [], volumes: []) {
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

  def pipelineRun() {
    bailOnUninitialized();

    getSteps().podTemplate(label: "CI-${application}", containers: [
      getSteps().containerTemplate(name: 'gke', image: 'gcr.io/sds-readiness/jenkins-gke:latest', ttyEnabled: true, command: 'cat', alwaysPullImage: true),
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
            // pull kubeconfig from GKE and init helm
            initKubeAndHelm()

            // build docker containers and push them with current SHA tag
            buildAndPushContainersWithTag(getScript().getGitSha())

            // inject new docker tags into default values.yaml
            setDefaultValues(getScript().getGitSha())

            // update chart semver metadata
            updateChartVersionMetadata(getScript().getGitSha())

            // if this is a Pull Request change
            if (getEnvironment().CHANGE_ID) {

              def testOverrides = getScript().getOverrides {
                overrides = [pipeline: getPipeline(), type: 'staging']
              }

              // lint, and deploy charts to staging namespace 
              // with injected docker tag values
              // and injected test values 
              // without uploading to helm repo
              lintHelmCharts()
              deployHelmChartsFromPath(
                'staging',  
                "${getPipeline().helm}-${getEnvironment().BUILD_NUMBER}",
                testOverrides
              )

              // test the deployed charts, destroy the deployments
              testHelmCharts(
                'staging', 
                "${getPipeline().helm}-${getEnvironment().BUILD_NUMBER}"
              )
            } else {
              // commit changes to Chart.yaml and vlaues.yaml to github
              pushChangesToGithub()

              // package and upload charts to helm repo
              uploadChartsToRepo()

              // if pipeline component is marked deployable,
              // deploy it.
              if (getPipeline().deploy) {
                def prodOverrides = getScript().getOverrides {
                  overrides = [pipeline: getPipeline(), type: 'prod']
                }
                upgradeHelmCharts(getScript().getGitSha(), prodOverrides)
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