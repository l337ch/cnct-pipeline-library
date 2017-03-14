#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipeline
  body()
  
  return getE2eVars(pipeline)
}

@NonCPS
def getE2eVars(pipeline) {
  def res = null
  if (pipeline.e2e) {
    def values = pipeline.e2e
    res = values.inject([]) { result, entry ->
      if (entry.value.toString().indexOf(' ') < 0) {
        result << "--${entry.key}=\"${entry.value.toString()}\""
      } else {
        result << "--${entry.key}=${entry.value.toString()}"
      }
    }.join(' ')
  }

  return res 
}