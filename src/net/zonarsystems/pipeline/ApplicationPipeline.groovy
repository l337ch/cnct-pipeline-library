package net.zonarsystems.pipeline
import com.cloudbees.groovy.cps.NonCPS

class ApplicationPipeline implements Pipeline {

  def pipelineCheckout() {
    getSteps().stage ('Checkout') {
      if (getEnv().CHANGE_ID != null) {
        helpers.githubPRCheckout(getApplication(), getEnv().CHANGE_ID)
      } else {
        if (getEnv().BRANCH_NAME != 'master') {
          error 'Aborting non-master branch build'
        }

        helpers.githubBranchCheckout(getApplication(), getEnv().BRANCH_NAME)
      }
    }
  }

  def upgradeHelmCharts(settings, pipeline) {
    stage ('Deploy Helm chart(s)') {
      chartsFolders = listFolders('./charts')
      for (i = 0; i < chartsFolders.size(); i++) {
        def chartName = chartsFolders[i].split('/').last()
        // TODO
        echo """TODO:
  helm repo add ${settings.githubOrg} ${settings.chartRepo}
  helm upgrade --install ${pipeline.get(PIPELINE).helm} ${settings.githubOrg}/${chartName}
  """
      }
    }
  }
}