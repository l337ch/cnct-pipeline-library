#!/usr/bin/env groovy
def getLibrary() {
  podTemplate(
    label: 'lib-init', 
    containers: [containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave:2.62-alpine', args: '${computer.jnlpmac} ${computer.name}'),], 
    volumes: []) {
    node ('lib-init') {
    
      if (env.CHANGE_ID) {
        print "testing library PR ${env.CHANGE_ID}"
        return new Tuple(library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}"), true) 
      } else {
        return new Tuple(library('pipeline'), false)
      }
    }
  }
}

def libTuple = getLibrary()

if (libTuple[1]) {
  applicationPipeline = libTuple[0].net.cnct.pipeline.ApplicationPipeline.new(
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
