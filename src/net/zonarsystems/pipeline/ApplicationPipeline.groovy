package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

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

  def upgradeHelmCharts(dependencyOverrides, dockerImagesTag) {
    bailOnUninitialized()

    getSteps().stage ('Deploy Helm chart(s)') {
      def chartsFolders = getScript().listFolders('./charts')
      def dockerfileFolders = getScript().listFolders('./rootfs')

      def dockerValues = []
      for (def i = 0; i < dockerfileFolders.size(); i++) {
        def imageName = dockerfileFolders[i].split('/').last()
        dockerValues <<  "images.${imageName}=${getSettings().dockerRegistry}/${imageName}:${dockerImagesTag}"
      }

      for (i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        def upgradeString = "helm upgrade --install ${getPipeline().helm} ${getSettings().githubOrg}/${chartName} --version ${getHelmChartVersion()} --set ${dockerValues.join(',')}"
        if (dependencyOverrides) {
          upgradeString += ",${dependencyOverrides}"
        }
        // TODO
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
          getSteps().sh "gcloud docker -- build -t ${getSettings().dockerRegistry}/${imageName}:${imageTag} --build-arg ARTIFACTORY_IP=${getHostIp(getSettings().artifactory)} ${dockerfileFolders[i]}"
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

  def deployHelmChartsFromPath(namespace, dockerImagesTag, releaseName, dependencyOverrides) {
    bailOnUninitialized()

    getSteps().stage ("Deploy Helm chart(s) to ${namespace} namespace") {
      def chartsFolders = getScript().listFolders('./charts')
      def dockerfileFolders = getScript().listFolders('./rootfs')

      def dockerValues = []
      for (def i = 0; i < dockerfileFolders.size(); i++) {
        def imageName = dockerfileFolders[i].split('/').last()
        dockerValues <<  "images.${imageName}=${getSettings().dockerRegistry}/${imageName}:${dockerImagesTag}"
      }

      for (def i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        if (getSteps().fileExists("${chartsFolders[i]}/Chart.yaml")) {
          def commandString = "helm install ${chartsFolders[i]} --name ${releaseName} --namespace ${namespace} --set ${dockerValues.join(',')}" 
          if (dependencyOverrides) {
            commandString += ",${dependencyOverrides}"
          }

          getSteps().echo "TODO: ${commandString}"
        }
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
          getSteps().echo "TODO: helm delete --purge ${releaseName}"
        }
      }
    }
  }

  def getHelmChartVersion() {
    bailOnUninitialized()

    chartYaml = getScript().parseYaml {
      yaml = readTrusted("charts/${application}/Chart.yaml")
    }

    return chartYaml.version
  }

  def incrementHelmChartVersion() {
    bailOnUninitialized()

    getSteps().stage ('Increment chart version') {
      getSteps().echo "TODO: Bump version in ./charts/${application} Chart.yaml"
    }
  }

  def setDependencyVersion(helmChart, dependencyName, dependencyVer) {
    bailOnUninitialized()

    getSteps().stage ('Set dependency version') {
      def reqYaml = getScript().parseYaml {
        yaml = readTrusted("charts/${helmChart}/requirements.yaml")
      }

      // set version
      for (def i = 0; i < reqYaml.get('dependencies').size(); i++) {
        if (reqYaml.get('dependencies')[i].name == dependencyName) {
          reqYaml.get('dependencies')[i].version = dependencyVer
        }
      }

      // dump YAML back
      def changedYAML = getScript().toYaml {
        obj = reqYaml
      }

      // write to file
      getSteps().writeFile(file: 'charts/${helmChart}/requirements.yaml', text: changedYAML)
    }
  }

  def pushChangesToGithub() {
    bailOnUninitialized()

    getSteps().stage ('Push chnages to github if needed') {
      getSteps().echo "TODO: push whatever has changed to github"
    }
  }

  def getUpstreamEnv() {
    bailOnUninitialized()

    def upstreamEnv = new EnvVars()
    def upstreamCause = getScript().currentBuild.rawBuild.getCause(Cause$UpstreamCause)

    if (upstreamCause) {
      def upstreamJobName = upstreamCause.properties.upstreamProject
      def upstreamBuild = Jenkins.instance
                              .getItemByFullName(upstreamJobName)
                              .getLastBuild()
      upstreamEnv = upstreamBuild.getAction(EnvActionImpl).getEnvironment()
    }

    return upstreamEnv
  }

  def isDownstreamBuild() {
    bailOnUninitialized()

    return getScript().currentBuild.rawBuild.getCause(Cause$UpstreamCause) != null
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

    // add triggers
    def triggers = []
    def upstream = getPipeline().upstream
    if (upstream) {
      for (def i = 0; i < upstream.size(); i++) {
        triggers << [
          $class: 'jenkins.triggers.ReverseBuildTrigger', 
          upstreamProjects: "${getPipeline(upstream[i]).pipeline}/master", 
          threshold: hudson.model.Result.SUCCESS
        ]
      } 
    }
    getSteps().properties([
      getSteps().pipelineTriggers([triggers: triggers])
    ])

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

        // get requirements update variables from upstream
        def reqVars = getScript().unpackReqVars {
          upstreamEnv = getUpstreamEnv()
        }

        // get helm overrides variable from upstream
        def depVars = getScript().unpackDependencyVars {
          upstreamEnv = getUpstreamEnv()
        }

        try {
          // Checkout source code, from PR or master
          pipelineCheckout()

          // Inside jenkins-gke tool container
          getSteps().container('gke') { 
            // pull kubeconfig from GKE and init helm
            initKubeAndHelm()

            // build docker containers and push them with current SHA tag
            buildAndPushContainersWithTag(getScript().getGitSha())

            // if this is a dependency update build, 
            if (depVars.size() > 0) {
              // set the version in requirements.yaml
              // if required
              if (reqVars.size() > 0) {
                reqVarKeys = new ArrayList(reqVars.keySet())
                for (def i = 0; i < reqVarKeys.size(); i++ ) {
                  setDependencyVersion(
                    application, 
                    reqVarKeys[i], 
                    reqVars.get(reqVarKeys[i])
                  )
                }
              }

              // lint, and deploy charts to test namespace 
              // with injected values without uploading to helm repo
              lintHelmCharts()
              deployHelmChartsFromPath(
                'staging', 
                getScript().getGitSha(), 
                "${getPipeline().helm}-${getEnvironment().BUILD_NUMBER}",
                depVars
              )

              incrementHelmChartVersion()
            } 

            if (!getEnvironment().CHANGE_ID) { // this is a merge commit
              // if this change involved changes to the Helm chart
              // or if this is a dependency update
              // increment the Helm chart version
              if (getScript().isChartChange(getScript().getGitSha()) || reqVars.size() > 0) {
                incrementHelmChartVersion()
              }
              
              // package and upload charts to helm repo
              uploadChartsToRepo()

              // if pipeline component is marked deployable,
              // deploy it.
              if (getPipeline().deploy) {
                upgradeHelmCharts(getScript().getGitSha(), depVars)
              }

              // set environment for downstream build (if any) to pickup
              if (getScript().isChartChange(getScript().getGitSha())) {
                getEnvironment().put("HELMREQ_${application}", getHelmChartVersion())
              }
              def dockerfileFolders = getScript().listFolders('./rootfs')
              for (def i = 0; i < dockerfileFolders.size(); i++) {
                def imageName = dockerfileFolders[i].split('/').last()
                getEnvironment().put(
                  "HELMPARAM_${application}_images_${imageName}", 
                  "${getSettings().dockerRegistry}/${imageName}:${dockerImagesTag}"
                )
              } 
            } else { // this is a PR commit
              // lint, and deploy charts to test namespace 
              // with injected values without uploading to helm repo
              lintHelmCharts()
              deployHelmChartsFromPath(
                'staging', 
                getScript().getGitSha(), 
                "${getPipeline().helm}-${getEnvironment().BUILD_NUMBER}",
                depVars
              )

              // test the deployed charts, destroy the deployments
              testHelmCharts(
                'staging', 
                "${getPipeline().helm}-${getEnvironment().BUILD_NUMBER}"
              )
            }

            // commit whatever was changed to github
            pushChangesToGithub()
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