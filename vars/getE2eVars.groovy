#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = e2e
  body()
  
  return getE2eVars(config.e2e)
}

@NonCPS
def getE2eVars(e2e) {
  def res = null
  res = e2e.inject([]) { result, entry ->
    if (entry.value.toString().indexOf(' ') < 0) {
      result << "--${entry.key}=\"${entry.value.toString()}\""
    } else {
      result << "--${entry.key}=${entry.value.toString()}"
    }
  }.join(' ')

  return res 
}