package net.zonarsystems.pipeline

import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import hudson.model.Cause

class ApplicationPipeline implements Pipeline, Serializable {

  ApplicationPipeline() {
    init();
  }

  def pipelineRun() {
    
  }
}