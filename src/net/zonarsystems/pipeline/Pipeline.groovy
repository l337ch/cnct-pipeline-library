package net.zonarsystems.pipeline

trait Pipeline {                           
  def steps
  def application

  private ready = false
  private def setReady() { ready = true }
  private def bailOnUninitialized() { if (!ready) { throw new Exception('Pipeline not initialized') } }
  def init() {
    steps.podTemplate(label: "env-${getApplication()}", containers: [], volumes: []) {
      steps.node ("env-${getApplication()}") {
        settings = fileLoader.fromGit(
          'settings', 
          'https://github.com/samsung-cnct/zonar-pipeline.git', 
          'master', 
          'repo-scan-access', 
          ''
        ).getConfig()
        setSettings(settings)

        pipeline = fileLoader.fromGit(
          'pipeline', 
          "https://github.com/samsung-cnct/zonar-pipeline.git", 
          'master', 
          "repo-scan-access", 
          ''
        ).getConfig()
        setPipeline(pipeline)

        setEnv(env)
        setHelpers()
      }
    }
  }

  private env
  def getEnv() { bailOnUninitialized(); env }
  private def setEnv(env) { this.env = env }

  private settings
  def getSettings() { bailOnUninitialized(); settings }
  private def setSettings(settings) { this.settings = settings }

  private pipeline
  def getPipeline() { bailOnUninitialized(); pipeline[application] }
  private def setPipeline(pipeline) { this.pipeline = pipeline }
  
  private helpers
  def getHelpers() { bailOnUninitialized(); helpers }
  private def setHelpers() { 
    if (!settings || !pipeline) {
      throw new Exception('Set settings and pipeline info first.')
    } 
    helpers = new PipelineHelpers(steps, settings, pipeline) 
  }

  abstract def pipelineRun()  
}