package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

class ApplicationPipeline extends Pipeline {
  ApplicationPipeline(steps, application) {
    this.steps = steps
    this.application = application
    init();
  }

  def pipelineRun() {
    
  }
}