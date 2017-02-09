package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

class ApplicationPipeline implements Serializable {
  def steps
  def application
  def env
  def settings

  def pipeline
  def getPipeline() { bailOnUninitialized(); pipeline[application] }

  def helpers
  
  def ready = false
  def bailOnUninitialized() { if (!ready) { throw new Exception('Pipeline not initialized, run init() first') } }

  ApplicationPipeline(steps, application) {
    this.steps = steps
    this.application = application
    init();
  }

  def pipelineCheckout() {
    bailOnUninitialized()

    getSteps().stage ('Checkout') {
      if (env.CHANGE_ID != null) {
        helpers.githubPRCheckout(application, env.CHANGE_ID)
      } else {
        if (env.BRANCH_NAME != 'master') {
          steps.error 'Aborting non-master branch build'
        }

        helpers.githubBranchCheckout(application, env.BRANCH_NAME)
      }
    }
  }

  def upgradeHelmCharts(dependencyOverrides) {
    bailOnUninitialized()

    steps.stage ('Deploy Helm chart(s)') {
      chartsFolders = listFolders('./charts')
      for (i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        def upgradeString = "helm upgrade --install ${getPipeline().helm} ${settings.githubOrg}/${chartName}"
        if (dependencyOverrides) {
          upgradeString += " --set ${dependencyOverrides}"
        }
        // TODO
        steps.echo """TODO:
  helm repo add ${settings.githubOrg} ${settings.chartRepo}
  ${upgradeString}
  """
      }
    }
  }

  def initKubeAndHelm() {
    bailOnUninitialized()

    steps.stage ('Init kubectl and helm') {
      steps.sh """
  gcloud container clusters get-credentials ${env.GKE_CLUSTER_NAME} --zone ${env.GKE_PRIMARY_ZONE} --project ${env.GKE_PROJECT_NAME}
  helm init
  """
    }
  }

  def buildAndPushContainersWithTag(imageTag) {
    bailOnUninitialized()

    steps.stage ('Build and push docker containers') {
      // if there is a docker file directly under rootfs - build it 
      // and name the GCR repo after the pipeline name
      // otherwise
      // enumerate all the folders under rootfs and build Dockerfiles in each one
      // name the GCR repo after foldername
      def dockerfileFolders = listFolders('./rootfs')
      for (i = 0; i < dockerfileFolders.size(); i++) {
        if (fileExists("${dockerfileFolders[i]}/Dockerfile")) {
          def imageName = dockerfileFolders[i].split('/').last()
          steps.sh "gcloud docker -- build -t ${settings.dockerRegistry}/${imageName}:${imageTag} --build-arg ARTIFACTORY_IP=${getHostIp(settings.artifactory)} ${dockerfileFolders[i]}"
          steps.sh "gcloud docker -- push ${settings.dockerRegistry}/${imageName}:${imageTag}"
        }
      }
    }
  }

  def lintHelmCharts() {
    bailOnUninitialized()

    steps.stage ('Lint Helm chart(s)') {
      steps.sh 'make lint -C charts'
    }
  }

  def uploadChartsToRepo() {
    bailOnUninitialized()

    steps.stage ('Upload Helm chart(s) to helm repo') {
      steps.sh """
  gcloud auth activate-service-account ${env.HELM_GKE_SERVICE_ACCOUNT} --key-file /etc/helm/helm-service-account.json
  make all -C charts
  gcloud auth activate-service-account ${env.MAIN_GKE_SERVICE_ACCOUNT} --key-file /etc/gke/service-account.json
  """
    }
  }

  def deployHelmChartsFromPath(namespace, dockerImagesTag, releaseName, dependencyOverrides) {
    bailOnUninitialized()

    steps.stage ("Deploy Helm chart(s) to ${namespace} namespace") {
      def chartsFolders = listFolders('./charts')
      def dockerfileFolders = listFolders('./rootfs')

      def dockerValues = []
      for (i = 0; i < dockerfileFolders.size(); i++) {
        def imageName = dockerfileFolders[i].split('/').last()
        dockerValues <<  "images.${imageName}=${settings.dockerRegistry}/${imageName}:${dockerImagesTag}"
      }

      for (i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        if (fileExists("${chartsFolders[i]}/Chart.yaml")) {
          def commandString = "helm install ${chartsFolders[i]} --name ${releaseName} --namespace ${namespace} --set ${dockerValues.join(',')}" 
          if (dependencyOverrides) {
            commandString += ",${dependencyOverrides}"
          }

          steps.echo "TODO: ${commandString}"
        }
      }
    }
  }

  def testHelmCharts(namespace, releaseName) {
    bailOnUninitialized()

    steps.stage ('Test Helm chart(s)') {
      chartsFolders = listFolders('./charts')
      for (i = 0; i < chartsFolders.size(); i++) {
        if (fileExists("${chartsFolders[i]}/Chart.yaml")) {
          steps.echo "TODO: test ${chartsFolders[i]} deployed to ${namespace} namespace"
          steps.echo "TODO: helm delete --purge ${releaseName}"
        }
      }
    }
  }

  def getHelmChartVersion() {
    bailOnUninitialized()

    chartYaml = parseYaml {
      yaml = readTrusted("charts/${application}/Chart.yaml")
    }

    return chartYaml.version
  }

  def incrementHelmChartVersion() {
    bailOnUninitialized()

    steps.stage ('Increment chart version') {
      steps.echo "TODO: Bump version in ./charts/${application} Chart.yaml and push to github"
    }
  }

  def setDependencyVersion(helmChart, dependencyName, dependencyVer) {
    bailOnUninitialized()

    steps.stage ('Set dependency version') {
      reqYaml = parseYaml {
        yaml = readTrusted("charts/${helmChart}/requirements.yaml")
      }

      // set version
      for (i = 0; i < reqYaml.get('dependencies').size(); i++) {
        if (reqYaml.get('dependencies')[i].name == dependencyName) {
          reqYaml.get('dependencies')[i].version = dependencyVer
        }
      }

      // dump YAML back
      def changedYAML = toYaml {
        obj = reqYaml
      }

      // write to file
      writeFile(file: 'charts/${helmChart}/requirements.yaml', text: changedYAML)

      // TODO: push changes back to github
    }
  }

  def getUpstreamEnv() {
    bailOnUninitialized()

    def upstreamEnv = new EnvVars()
    def upstreamCause = currentBuild.rawBuild.getCause(Cause$UpstreamCause)

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
    
    return currentBuild.rawBuild.getCause(Cause$UpstreamCause) != null
  }

  // init things that need node context
  def init() {
    steps.podTemplate(label: "env-${application}", containers: [], volumes: []) {
      steps.node ("env-${application}") {
        settings = fileLoader.fromGit(
          'settings', 
          'https://github.com/samsung-cnct/zonar-pipeline.git', 
          'master', 
          'repo-scan-access', 
          ''
        ).getConfig()

        pipeline = fileLoader.fromGit(
          'pipeline', 
          "https://github.com/samsung-cnct/zonar-pipeline.git", 
          'master', 
          "repo-scan-access", 
          ''
        ).getConfig()

        this.env = env
        helpers = new PipelineHelpers(steps, settings, pipeline)
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
      for (i = 0; i < upstream.size(); i++) {
        triggers << [
          $class: 'jenkins.triggers.ReverseBuildTrigger', 
          upstreamProjects: "${pipeline.get(upstream[i]).pipeline}/master", 
          threshold: hudson.model.Result.SUCCESS
        ]
      } 
    }
    steps.properties([
      pipelineTriggers([triggers: triggers])
    ])

    steps.podTemplate(label: "CI-${application}", containers: [
      containerTemplate(name: 'gke', image: 'gcr.io/sds-readiness/jenkins-gke:latest', ttyEnabled: true, command: 'cat', alwaysPullImage: true),
    ],
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
      secretVolume(mountPath: '/etc/gke', secretName: "jenkins-gke-${env.UNIQUE_JENKINS_ID}"),
      secretVolume(mountPath: '/etc/helm', secretName: "jenkins-helm-${env.UNIQUE_JENKINS_ID}")
    ]) {
      
      steps.node ("CI-${application}") {
        def err = null
        
        // default message
        def notifyMessage = 'Build succeeded for ' + "${env.JOB_NAME} number ${env.BUILD_NUMBER} (${env.BUILD_URL})"
        def notifyColor = 'good'

        // get requirements update variables from upstream
        reqVars = unpackReqVars {
          upstreamEnv = getUpstreamEnv()
        }

        // get helm overrides variable from upstream
        depVars = unpackDependencyVars {
          upstreamEnv = getUpstreamEnv()
        }

        try {
          // Checkout source code, from PR or master
          pipelineCheckout()

          // Inside jenkins-gke tool container
          steps.container('gke') { 
            // pull kubeconfig from GKE and init helm
            initKubeAndHelm()

            // if this is a dependency update build, 
            // set the version in requirements.yaml
            // and push to github
            if (reqVars.size() > 0) {
              reqVarKeys = new ArrayList(reqVars.keySet())
              for (i = 0; i < reqVarKeys.size(); i++ ) {
                setDependencyVersion(
                  application, 
                  reqVarKeys[i], 
                  reqVars.get(reqVarKeys[i])
                )
              }
            } else {
              // if this is an open PR
              if (env.CHANGE_ID != null) {
                // build docker containers and push them with current SHA tag
                buildAndPushContainersWithTag(getGitSha())
              
                // lint, and deploy charts to test namespace 
                // with injected values without uploading to helm repo
                lintHelmCharts()
                deployHelmChartsFromPath(
                  'staging', 
                  getGitSha(), 
                  "${pipeline.get(PIPELINE).helm}-${env.BUILD_NUMBER}",
                  depVars
                )

                // test the deployed charts, destroy the deployments
                testHelmCharts(
                  'staging', 
                  "${pipeline.get(PIPELINE).helm}-${env.BUILD_NUMBER}"
                )
              } else {
                // build docker containers and push them with current SHA tag
                buildAndPushContainersWithTag(getGitSha())
              } 
            }

            // If this is a merge commit 
            if (env.CHANGE_ID == null) {

              // if this change involved changes to the Helm chart
              // or if this is a dependency update
              // increment the Helm chart version and push to github
              if (isChartChange(getGitSha()) || reqVars.size() > 0) {
                incrementHelmChartVersion()
              }
              
              // package and upload charts to helm repo
              uploadChartsToRepo()

              // if pipeline component is marked deployable,
              // deploy it.
              if (getPipeline().deploy) {
                upgradeHelmCharts(depVars)
              }

              // set environment for downstream build (if any) to pickup
              if (isChartChange(getGitSha())) {
                env.put("HELMREQ_${application}", getHelmChartVersion())
              }
              def dockerfileFolders = listFolders('./rootfs')
              for (i = 0; i < dockerfileFolders.size(); i++) {
                def imageName = dockerfileFolders[i].split('/').last()
                env.put(
                  "HELMPARAM_${application}_images_${imageName}", 
                  "${settings.dockerRegistry}/${imageName}:${dockerImagesTag}"
                )
              } 
            } 
          }
        } catch (e) {
          currentBuild.result = 'FAILURE'
          notifyMessage = 'Build failed for ' + "${env.JOB_NAME} number ${env.BUILD_NUMBER} (${env.BUILD_URL}) : ${e.getMessage()}"
          notifyColor = 'danger'
          err = e
        } finally {
          steps.stage ('Notify') {
            helpers.sendSlack(
              pipeline.pipeline.slack,
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