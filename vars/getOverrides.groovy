#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return getOverrides(config.overrides.pipeline, config.overrides.type)
}

@NonCPS
def getOverrides(pipeline, overrideType) {
  def res = null
  if (pipeline.overrides) {
    def overrides = pipeline.overrides.get(overrideType)
    if (overrides) {
      res = overrides.inject([]) { result, entry ->
        result << "${entry.key}=${entry.value.toString()}"
      }.join(',')
    }
  }

  return res 
}