package net.zonarsystems.pipeline

trait Pipeline implements Serializable {                           
  def steps
  def application
  def env

  private settings = fileLoader.fromGit(
    'settings', 
    'https://github.com/samsung-cnct/zonar-pipeline.git', 
    'master', 
    'repo-scan-access', 
    ''
  ).getConfig()
  private pipeline = fileLoader.fromGit(
    'pipeline', 
    "https://github.com/samsung-cnct/zonar-pipeline.git", 
    'master', 
    "repo-scan-access", 
    ''
  ).getConfig() 

  private helpers = new PipelineHelpers(
    this.steps, 
    this.settings, 
    this.pipeline
  )

  def getSettings() { settings }
  def getPipeline() { pipeline }
  def getHelpers() { helpers }
}