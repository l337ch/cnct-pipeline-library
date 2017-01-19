package com.zonarsystems.pipeline

class PipelineHelpers implements Serializable {
  def steps
  Map settings
  Map pipeline

  PipelineHelpers(steps, settings, pipeline) {
    this.steps = steps
    this.settings = settings
    this.pipeline = pipeline
  }

  def getGitRepo(repoName) {
    return 'git@github.com:' + properties.settings.githubOrg + '/' + repoName + '.git'
  }

  def githubCheckout(repoName, branchName) {
    steps.git(
      branch: branchName, 
      credentialsId: settings.get("githubCredentials"), 
      url: getGitRepo(repoName))
  }

  def githubPipelineCheckout(branchName) {
    githubCheckout(settings.get("pipelineRepo"), branchName)
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
    message = 'Build succeeded for ' + jobInfo
    sendSlack(pipeline.get("name").get("slack"), message, 'good')
  }

  def sendSlackFail(name, jobInfo) {
    message = 'Build failed for ' + jobInfo
    sendSlack(pipeline.get("name").get("slack"), message, 'danger')
  }
}