#!/usr/bin/env groovy
def getLibrary() {
  podTemplate(
    label: 'lib-init', 
    containers: [containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave:2.62-alpine', args: '${computer.jnlpmac} ${computer.name}'),], 
    volumes: []) {
    getSteps().node ('lib-init') {
      if (env.CHANGE_ID) {
        print "testing library PR ${env.CHANGE_ID}"
        return library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}")
      } else {
        print 'Testing library on master'
        return library('pipeline')
      }
    }
  }
}
def lib = getLibrary()
applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new(
  steps, 
  'pipelinelibrary', 
  this
)
applicationPipeline.init()
applicationPipeline.pipelineRun()
