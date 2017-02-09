package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

class ApplicationPipeline implements Pipeline, Serializable {

  def pipelineCheckout() {
    getSteps().stage ('Checkout') {
      if (getEnv().CHANGE_ID != null) {
        getHelpers().githubPRCheckout(getApplication(), getEnv().CHANGE_ID)
      } else {
        if (getEnv().BRANCH_NAME != 'master') {
          getSteps().error 'Aborting non-master branch build'
        }

        getHelpers().githubBranchCheckout(getApplication(), getEnv().BRANCH_NAME)
      }
    }
  }

  def upgradeHelmCharts(dependencyOverrides) {
    getSteps().stage ('Deploy Helm chart(s)') {
      chartsFolders = listFolders('./charts')
      for (i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        def upgradeString = "helm upgrade --install ${getPipeline().helm} ${getSettings().githubOrg}/${chartName}"
        if (dependencyOverrides) {
          upgradeString += " --set ${dependencyOverrides}"
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
    getSteps().stage ('Init kubectl and helm') {
      getSteps().sh """
  gcloud container clusters get-credentials ${getEnv().GKE_CLUSTER_NAME} --zone ${getEnv().GKE_PRIMARY_ZONE} --project ${getEnv().GKE_PROJECT_NAME}
  helm init
  """
    }
  }

  def buildAndPushContainersWithTag(imageTag) {
    getSteps().stage ('Build and push docker containers') {
      // if there is a docker file directly under rootfs - build it 
      // and name the GCR repo after the pipeline name
      // otherwise
      // enumerate all the folders under rootfs and build Dockerfiles in each one
      // name the GCR repo after foldername
      def dockerfileFolders = listFolders('./rootfs')
      for (i = 0; i < dockerfileFolders.size(); i++) {
        if (fileExists("${dockerfileFolders[i]}/Dockerfile")) {
          def imageName = dockerfileFolders[i].split('/').last()
          getSteps().sh "gcloud docker -- build -t ${getSettings().dockerRegistry}/${imageName}:${imageTag} --build-arg ARTIFACTORY_IP=${getHostIp(getSettings().artifactory)} ${dockerfileFolders[i]}"
          getSteps().sh "gcloud docker -- push ${getSettings().dockerRegistry}/${imageName}:${imageTag}"
        }
      }
    }
  }

  def lintHelmCharts() {
    getSteps().stage ('Lint Helm chart(s)') {
      getSteps().sh 'make lint -C charts'
    }
  }

  def uploadChartsToRepo() {
    getSteps().stage ('Upload Helm chart(s) to helm repo') {
      getSteps().sh """
  gcloud auth activate-service-account ${getEnv().HELM_GKE_SERVICE_ACCOUNT} --key-file /etc/helm/helm-service-account.json
  make all -C charts
  gcloud auth activate-service-account ${getEnv().MAIN_GKE_SERVICE_ACCOUNT} --key-file /etc/gke/service-account.json
  """
    }
  }

  def deployHelmChartsFromPath(namespace, dockerImagesTag, releaseName, dependencyOverrides) {
    getSteps().stage ("Deploy Helm chart(s) to ${namespace} namespace") {
      def chartsFolders = listFolders('./charts')
      def dockerfileFolders = listFolders('./rootfs')

      def dockerValues = []
      for (i = 0; i < dockerfileFolders.size(); i++) {
        def imageName = dockerfileFolders[i].split('/').last()
        dockerValues <<  "images.${imageName}=${getSettings().dockerRegistry}/${imageName}:${dockerImagesTag}"
      }

      for (i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        if (fileExists("${chartsFolders[i]}/Chart.yaml")) {
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
    getSteps().stage ('Test Helm chart(s)') {
      chartsFolders = listFolders('./charts')
      for (i = 0; i < chartsFolders.size(); i++) {
        if (fileExists("${chartsFolders[i]}/Chart.yaml")) {
          getSteps().echo "TODO: test ${chartsFolders[i]} deployed to ${namespace} namespace"
          getSteps().echo "TODO: helm delete --purge ${releaseName}"
        }
      }
    }
  }

  def getHelmChartVersion() {
    chartYaml = parseYaml {
      yaml = readTrusted("charts/${getApplication()}/Chart.yaml")
    }

    return chartYaml.version
  }

  def incrementHelmChartVersion() {
    getSteps().stage ('Increment chart version') {
      getSteps().echo "TODO: Bump version in ./charts/${getApplication()} Chart.yaml and push to github"
    }
  }

  def setDependencyVersion(helmChart, dependencyName, dependencyVer) {
    getSteps().stage ('Set dependency version') {
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
    return currentBuild.rawBuild.getCause(Cause$UpstreamCause) != null
  }

  def pipelineRun() {

    // init 
    init()

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
    getSteps().properties([
      pipelineTriggers([triggers: triggers])
    ])

    getSteps().podTemplate(label: "CI-${getApplication()}", containers: [
      containerTemplate(name: 'gke', image: 'gcr.io/sds-readiness/jenkins-gke:latest', ttyEnabled: true, command: 'cat', alwaysPullImage: true),
    ],
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
      secretVolume(mountPath: '/etc/gke', secretName: "jenkins-gke-${getEnv().UNIQUE_JENKINS_ID}"),
      secretVolume(mountPath: '/etc/helm', secretName: "jenkins-helm-${getEnv().UNIQUE_JENKINS_ID}")
    ]) {
      
      getSteps().node ("CI-${getApplication()}") {
        def err = null
        
        // default message
        def notifyMessage = 'Build succeeded for ' + "${getEnv().JOB_NAME} number ${getEnv().BUILD_NUMBER} (${getEnv().BUILD_URL})"
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
          getSteps().container('gke') { 
            // pull kubeconfig from GKE and init helm
            initKubeAndHelm()

            // if this is a dependency update build, 
            // set the version in requirements.yaml
            // and push to github
            if (reqVars.size() > 0) {
              reqVarKeys = new ArrayList(reqVars.keySet())
              for (i = 0; i < reqVarKeys.size(); i++ ) {
                setDependencyVersion(
                  getApplication(), 
                  reqVarKeys[i], 
                  reqVars.get(reqVarKeys[i])
                )
              }
            } else {
              // if this is an open PR
              if (getEnv().CHANGE_ID != null) {
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
            if (getEnv().CHANGE_ID == null) {

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
                getEnv().put("HELMREQ_${getApplication()}", getHelmChartVersion())
              }
              def dockerfileFolders = listFolders('./rootfs')
              for (i = 0; i < dockerfileFolders.size(); i++) {
                def imageName = dockerfileFolders[i].split('/').last()
                getEnv().put(
                  "HELMPARAM_${getApplication()}_images_${imageName}", 
                  "${getSettings().dockerRegistry}/${imageName}:${dockerImagesTag}"
                )
              } 
            } 
          }
        } catch (e) {
          currentBuild.result = 'FAILURE'
          notifyMessage = 'Build failed for ' + "${getEnv().JOB_NAME} number ${getEnv().BUILD_NUMBER} (${getEnv().BUILD_URL}) : ${e.getMessage()}"
          notifyColor = 'danger'
          err = e
        } finally {
          getSteps().stage ('Notify') {
            getHelpers().sendSlack(
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