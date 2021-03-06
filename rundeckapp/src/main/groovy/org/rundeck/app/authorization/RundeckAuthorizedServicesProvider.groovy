/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.cluster.ClusterInfoService
import com.dtolabs.rundeck.core.jobs.JobService
import groovy.transform.CompileStatic
import org.rundeck.app.spi.AppService
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import org.rundeck.app.spi.ServicesProvider
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FrameworkService
import rundeck.services.JobStateService

@CompileStatic
class RundeckAuthorizedServicesProvider implements AuthorizedServicesProvider {
    @Autowired JobStateService jobStateService
    ServicesProvider baseServices
    private static List<Class> SERVICE_TYPES = [(Class) JobService]

    @Override
    Services getServicesWith(final AuthContext authContext) {
        return new AuthedServices(authContext, baseServices.services)
    }

    class AuthedServices implements Services {
        final AuthContext authContext
        final Services baseServices

        AuthedServices(final AuthContext authContext, final Services baseServices) {
            this.authContext = authContext
            this.baseServices = baseServices
        }

        @Override
        boolean hasService(final Class<? extends AppService> type) {
            baseServices.hasService(type) || type in SERVICE_TYPES
        }

        @Override
        def <T extends AppService> T getService(final Class<T> type) {
            if (baseServices.hasService(type)) {
                return baseServices.getService(type)
            }
            if (type == JobService) {
                return (T) jobStateService.jobServiceWithAuthContext(authContext)
            }
            throw new IllegalStateException("Required service " + type.getName() + " was not available");
        }
    }
}
