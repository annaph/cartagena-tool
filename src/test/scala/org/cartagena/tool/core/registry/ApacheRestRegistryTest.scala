package org.cartagena.tool.core.registry

import org.cartagena.tool.core.http.RestHelper
import org.cartagena.tool.core.http.apache.{ApacheHttpClient, ApacheHttpClientComponent, ApacheHttpOperations, ApacheHttpOperationsComponent, ApacheRestHelperComponent}
import org.scalatestplus.mockito.MockitoSugar

trait ApacheRestRegistryTest
  extends ApacheHttpClientComponent
    with ApacheHttpOperationsComponent
    with ApacheRestHelperComponent
    with MockitoSugar {

  override private[core] val apacheHttpClient =
    mock[ApacheHttpClient]

  override private[core] val apacheHttpOperations =
    mock[ApacheHttpOperations]

  override private[core] val apacheHttpRestHelper: RestHelper =
    mock[RestHelper]

}
