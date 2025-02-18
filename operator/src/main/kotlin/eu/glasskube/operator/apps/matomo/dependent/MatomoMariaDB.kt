package eu.glasskube.operator.apps.matomo.dependent

import eu.glasskube.kubernetes.api.model.metadata
import eu.glasskube.operator.apps.matomo.Matomo
import eu.glasskube.operator.apps.matomo.MatomoReconciler
import eu.glasskube.operator.apps.matomo.databaseName
import eu.glasskube.operator.apps.matomo.databaseSecretName
import eu.glasskube.operator.apps.matomo.databaseUser
import eu.glasskube.operator.apps.matomo.genericMariaDBName
import eu.glasskube.operator.apps.matomo.resourceLabels
import eu.glasskube.operator.config.ConfigKey
import eu.glasskube.operator.config.ConfigService
import eu.glasskube.operator.generic.condition.MariaDBReadyCondition
import eu.glasskube.operator.infra.mariadb.Exporter
import eu.glasskube.operator.infra.mariadb.MariaDB
import eu.glasskube.operator.infra.mariadb.MariaDBImage
import eu.glasskube.operator.infra.mariadb.MariaDBResources
import eu.glasskube.operator.infra.mariadb.MariaDBResourcesRequest
import eu.glasskube.operator.infra.mariadb.MariaDBSpec
import eu.glasskube.operator.infra.mariadb.MariaDBVolumeClaimTemplate
import eu.glasskube.operator.infra.mariadb.Metrics
import eu.glasskube.operator.infra.mariadb.ServiceMonitor
import eu.glasskube.operator.infra.mariadb.mariaDB
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirements
import io.fabric8.kubernetes.api.model.SecretKeySelector
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent

@KubernetesDependent(labelSelector = MatomoReconciler.SELECTOR)
class MatomoMariaDB(private val configService: ConfigService) :
    CRUDKubernetesDependentResource<MariaDB, Matomo>(MariaDB::class.java) {

    class ReadyPostCondition : MariaDBReadyCondition<Matomo>()

    override fun desired(primary: Matomo, context: Context<Matomo>) = mariaDB {
        metadata {
            name = primary.genericMariaDBName
            namespace = primary.metadata.namespace
            labels = primary.resourceLabels
        }
        spec = MariaDBSpec(
            rootPasswordSecretKeyRef = SecretKeySelector("ROOT_DATABASE_PASSWORD", primary.databaseSecretName, null),
            image = MariaDBImage("mariadb", "10.7.4", "IfNotPresent"),
            database = primary.databaseName,
            username = primary.databaseUser,
            passwordSecretKeyRef = SecretKeySelector("MATOMO_DATABASE_PASSWORD", primary.databaseSecretName, null),
            volumeClaimTemplate = MariaDBVolumeClaimTemplate(
                resources = MariaDBResources(MariaDBResourcesRequest("10Gi")),
                storageClassName = configService.getValue(ConfigKey.databaseStorageClassName)
            ),
            resources = ResourceRequirements(
                null,
                mapOf("memory" to Quantity("512", "Mi")),
                mapOf("memory" to Quantity("256", "Mi"))
            ),
            metrics = Metrics(
                exporter = Exporter(
                    image = MariaDBImage("prom/mysqld-exporter", "v0.14.0", "IfNotPresent")
                ),
                serviceMonitor = ServiceMonitor(
                    prometheusRelease = "kube-prometheus-stack"
                )
            )
        )
    }
}
