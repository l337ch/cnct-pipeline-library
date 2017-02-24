#!/usr/bin/env groovy

def libspec = '''
@Library(\'pipeline@master\')
import net.zonarsystems.pipeline.ApplicationPipeline'''

podTemplate(label: "env-pipelinelibrary", containers: [], volumes: []) {
  node ("env-pipelinelibrary") {
    if (env.CHANGE_ID) {
      libspec = """
      @Library('pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}')
      import net.zonarsystems.pipeline.ApplicationPipeline"""
    } 
  }
}

libspec += '''

def runGeneratedSource() {
  applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
  applicationPipeline.init()
  applicationPipeline.pipelineRun()
}

return this;
'''

def generatedPipeline = null
podTemplate(label: "env-pipelinelibrary", containers: [], volumes: []) {
  node ("env-pipelinelibrary") { 
    writeFile(file: 'Jenkinsfile.groovy', text: libspec)
    generatedPipeline = load 'Jenkinsfile.groovy'
  }
}
generatedPipeline.runGeneratedSource()
