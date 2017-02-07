package net.zonarsystems.pipeline

class PipelineHelpers implements Serializable {
  def steps
  def settings
  def pipeline

  PipelineHelpers(steps, settings, pipeline) {
    this.steps = steps
    this.settings = settings
    this.pipeline = pipeline
  }

  def getGitRepo(pipelineApp) {
    return "git@github.com:${settings.githubOrg}/" + pipeline.get(pipelineApp).repo + ".git"
  }

  def getHttpsRepo(pipelineApp) {
    return "https://github.com/${settings.githubOrg}/" + pipeline.get(pipelineApp).repo
  }

  def githubPRCheckout(pipelineApp, prId) {
    steps.checkout(
      [
        $class: 'GitSCM', 
        branches: [
          [name: "origin/pr/${prId}"]
        ], 
        doGenerateSubmoduleConfigurations: false, 
        extensions: [], 
        submoduleCfg: [], 
        userRemoteConfigs: [
          [
            credentialsId: settings.githubScanCredentials, 
            refspec: "+refs/pull/*/head:refs/remotes/origin/pr/*", 
            url: getHttpsRepo(pipelineApp)
          ]
        ]
      ]
    )
  }

  def githubBranchCheckout(pipelineApp, branchName) {
    steps.checkout(
      [
        $class: 'GitSCM', 
        branches: [
          [name: "refs/heads/${branchName}"]
        ], 
        doGenerateSubmoduleConfigurations: false, 
        extensions: [], 
        submoduleCfg: [], 
        userRemoteConfigs: [
          [
            credentialsId: settings.githubScanCredentials, 
            url: getHttpsRepo(pipelineApp)
          ]
        ]
      ]
    )
  }

  def sendSlack(channel, message, color) {
    steps.slackSend( 
      channel: channel, 
      color: color, 
      failOnError: true, 
      message: message, 
      teamDomain: settings.get("slackOrg"), 
      tokenCredentialId: settings.get("slackCredentials"))
  }

  def sendSlackOk(name, jobInfo) {
    sendSlack(
      pipeline.get(name).get("slack"), 
      'Build succeeded for ' + jobInfo, 
      'good')
  }

  def sendSlackFail(name, jobInfo) {
    sendSlack(
      pipeline.get(name).get("slack"), 
      'Build failed for ' + jobInfo, 
      'danger')
  }
}