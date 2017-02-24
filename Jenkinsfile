#!/usr/bin/env groovy

def libspec = '''
@Library(pipeline@master)
import net.zonarsystems.pipeline.ApplicationPipeline'''

podTemplate(label: "env-pipelinelibrary", containers: [], volumes: []) {
  node ("env-${application}") {
    if (env.CHANGE_ID) {
      libspec = """
      @Library(pipeline@refs/remotes/origin/pr/${env.CHANGE_ID})
      import net.zonarsystems.pipeline.ApplicationPipeline"""
    } 
  }
}

libspec += '''
applicationPipeline = new ApplicationPipeline(steps, \'pipelinelibrary\', this)
applicationPipeline.init()
applicationPipeline.pipelineRun()'''

evaluate(libspec)