#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return unpackReqVars(config.upstreamEnv)
}

@NonCPS
def unpackReqVars (upstreamEnv) {
  return upstreamEnv.inject([:]) { list, entry ->
    def result = [:]
    if (entry.key.startsWith('HELMREQ_')) {
      result << [(entry.key.substring(8)): entry.value.toString()]
    } 
    list += result
  }
}