package net.zonarsystems.pipeline

abstract class Pipeline implements Serializable {                           
  def steps
  def application
  def ready = false
  def bailOnUninitialized() { if (!ready) { throw new Exception('Pipeline not initialized, run init() first') } }

  def env
  def getEnv() { bailOnUninitialized(); env }

  def settings
  def getSettings() { bailOnUninitialized(); settings }
  
  def pipeline
  def getPipeline() { bailOnUninitialized(); pipeline[application] }
  
  def helpers
  def getHelpers() { bailOnUninitialized(); helpers }

  // init things that need node context
  def init() {
    steps.podTemplate(label: "env-${getApplication()}", containers: [], volumes: []) {
      steps.node ("env-${getApplication()}") {
        this.settings = fileLoader.fromGit(
          'settings', 
          'https://github.com/samsung-cnct/zonar-pipeline.git', 
          'master', 
          'repo-scan-access', 
          ''
        ).getConfig()

        this.pipeline = fileLoader.fromGit(
          'pipeline', 
          "https://github.com/samsung-cnct/zonar-pipeline.git", 
          'master', 
          "repo-scan-access", 
          ''
        ).getConfig()

        this.env = env
        this.helpers = new PipelineHelpers(steps, settings, pipeline)
      }
    }

    // set ready
    ready = true
  }

  abstract def pipelineRun()  
}