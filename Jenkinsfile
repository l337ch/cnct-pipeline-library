#!/usr/bin/env groovy

def libspec = 'pipeline@master'
podTemplate(label: "env-pipelinelibrary", containers: [], volumes: []) {
  node ("env-${application}") {
    if (env.CHANGE_ID) {
      libspec = 'pipeline@refs/remotes/origin/pr/' + env.CHANGE_ID
    } 
  }
}

@Library([libspec])
import net.zonarsystems.pipeline.ApplicationPipeline

applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
applicationPipeline.init()
applicationPipeline.pipelineRun()