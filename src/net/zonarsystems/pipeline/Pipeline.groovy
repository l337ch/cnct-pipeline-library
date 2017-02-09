package net.zonarsystems.pipeline

trait Pipeline {                           

  private ready = false
  private def bailOnUninitialized() { if (!ready) { throw new Exception('Pipeline not initialized, run init() first') } }

  private env
  def getEnv() { bailOnUninitialized(); env }

  private settings
  def getSettings() { bailOnUninitialized(); settings }
  
  private pipeline
  def getPipeline(application) { bailOnUninitialized(); pipeline[application] }
  
  private helpers
  def getHelpers() { bailOnUninitialized(); helpers }

  // init things that need node context
  def init(steps) {
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