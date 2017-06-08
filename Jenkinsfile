#!/usr/bin/env groovy
def getLibrary() {
  podTemplate(
    label: 'lib-init', 
    containers: [containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave:2.62-alpine', args: '${computer.jnlpmac} ${computer.name}'),], 
    volumes: []) {
    node ('lib-init') {
    
      if (env.CHANGE_ID) {
        print "testing library PR ${env.CHANGE_ID}"
        return library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}")
      } else {
        return library('pipeline')
      }
    }
  }
}

def isPRBuild() {
  podTemplate(
    label: 'lib-init', 
    containers: [containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave:2.62-alpine', args: '${computer.jnlpmac} ${computer.name}'),], 
    volumes: []) {
    node ('lib-init') {
    
      if (env.CHANGE_ID) {
        return true
      } else {
        return false
      }
    }
  }
}

if (isPRBuild()) {
  def lib = getLibrary()
  applicationPipeline = lib.net.cnct.pipeline.ApplicationPipeline.new(
    steps, 
    'pipelinelibrary', 
    this,
    [],
    [],
    true
  )
  applicationPipeline.init()
  applicationPipeline.pipelineRun()
}
