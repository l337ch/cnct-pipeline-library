#!/usr/bin/env groovy

def branch = ''
podTemplate(label: "env-pipelinelibrary", containers: [], volumes: []) {
  node ("env-${application}") {
    if (env.CHANGE_ID) {
      branch = "refs/remotes/origin/pr/${env.CHANGE_ID}"
    } else {
      branch = 'master'
    }
  }
}

@Library("pipeline@${branch}")
import net.zonarsystems.pipeline.ApplicationPipeline

applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
applicationPipeline.init()
applicationPipeline.pipelineRun()