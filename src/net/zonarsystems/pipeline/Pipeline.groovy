package net.zonarsystems.pipeline

trait Pipeline {                           
  def steps
  def application

  // init things that need node context
  def init() {
    
  }

  abstract def pipelineRun()  
}