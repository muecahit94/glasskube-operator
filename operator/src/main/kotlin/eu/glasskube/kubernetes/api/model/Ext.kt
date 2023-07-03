package eu.glasskube.kubernetes.api.model

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.IntOrString

fun String.intOrString() = IntOrString(this)
fun Int.intOrString() = IntOrString(this)

val HasMetadata.namespace: String?
    get() = metadata.namespace
