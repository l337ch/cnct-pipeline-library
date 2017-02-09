package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

class ApplicationPipeline implements Serializable {
  def steps
  def application

  ApplicationPipeline(steps, application) {
    this.steps = steps
    this.application = application
  }

  def pipelineRun() {
    
  }
}