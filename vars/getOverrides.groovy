#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return unpackDependencyVars(config.pipeline, config.type)
}

@NonCPS
def getOverrides(pipeline, overrideType) {
  def overrides = pipeline.overrides.get(overrideType)
  return overrides.inject([]) { result, entry ->
    result << "${entry.key}=${entry.value.toString()}"
  }.join(',')
}