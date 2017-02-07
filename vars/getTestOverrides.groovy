#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return unpackDependencyVars(config.pipeline)
}

@NonCPS
def getTestOverrides(pipeline) {
  def testOverrides = pipeline.get('test')
  return testOverrides.inject([]) { result, entry ->
    result << "${entry.key}=${entry.value.toString()}"
  }.join(',')
}