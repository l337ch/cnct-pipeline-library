package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

class ApplicationPipeline implements Pipeline, Serializable {
  def steps
  def application

  ApplicationPipeline(steps, application) {
    this.steps = steps
    this.application = application
    init();
  }

  def pipelineRun() {
    
  }
}