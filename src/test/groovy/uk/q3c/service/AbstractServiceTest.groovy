/*
 *
 *  * Copyright (c) 2016. David Sowerby
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package uk.q3c.service

import com.google.inject.Inject
import spock.lang.Specification
import uk.q3c.krail.eventbus.MessageBus
import uk.q3c.krail.i18n.I18NKey
import uk.q3c.krail.i18n.Translate
import uk.q3c.util.guice.SerializationSupport

/**
 * Created by David Sowerby on 08/11/15.
 *
 */
//@UseModules([])
class AbstractServiceTest extends Specification {

    def translate = Mock(Translate)
    SerializationSupport serializationSupport = Mock(SerializationSupport)

    TestService service

    def servicesModel = Mock(ServiceModel)
    MessageBus messageBus = Mock(MessageBus)
    RelatedServiceExecutor servicesExecutor = Mock(RelatedServiceExecutor)

    def setup() {

        service = new TestService(translate, messageBus, servicesExecutor, serializationSupport)
        service.setThrowStartException(false)
        service.setThrowStopException(false)

    }

    def "name translated"() {
        given:
        translate.from(LabelKey.Authorisation) >> "Authorisation"
        expect:
        service.getNameKey().equals(LabelKey.Authorisation)
        service.getName().equals("Authorisation")
    }


    def "description key translated"() {
        given:
        translate.from(LabelKey.Authorisation) >> "Authorisation"
        service.setDescriptionKey(LabelKey.Authorisation)

        expect:
        service.getDescriptionKey().equals(LabelKey.Authorisation)
        service.getDescription().equals("Authorisation")
    }


    def "setDescriptionKey null is accepted"() {
        when:
        service.setDescriptionKey(null)

        then:

        service.getDescriptionKey().equals(null)
    }

    def "missing description key returns empty String"() {

        expect:
        service.getDescription().equals("")
    }

    def "start"(Service.State initialState, RelatedServiceExecutor.Action action, boolean serviceFail, boolean allDepsOk, Service.Cause callWithCause, Service.State transientState, Service.State finalState, Service.Cause finalCause) {

        given:

        service.state = initialState
        service.throwStartException serviceFail

        when:

        ServiceStatus status = service.start(callWithCause)

        then:
        1 * messageBus.publishSync({ ServiceBusMessage m -> m.toState == transientState && m.cause == callWithCause })
        1 * servicesExecutor.execute(action, callWithCause) >> allDepsOk
        1 * messageBus.publishSync({ ServiceBusMessage m -> m.toState == finalState && m.cause == finalCause })
        service.getState() == finalState
        service.getCause() == finalCause
        status.state == finalState
        status.cause == finalCause
        status.service == service


        where:
        initialState          | action                              | serviceFail | allDepsOk | callWithCause         | transientState         | finalState           | finalCause
//        State.INITIAL | Action.START | false       | true      | Cause.STARTED | State.STARTING | State.RUNNING | Cause.STARTED
//        State.STOPPED | Action.START | false       | true      | Cause.STARTED | State.STARTING | State.RUNNING | Cause.STARTED
        Service.State.STOPPED | RelatedServiceExecutor.Action.START | true        | true      | Service.Cause.STARTED | Service.State.STARTING | Service.State.FAILED | Service.Cause.FAILED_TO_START
//        State.INITIAL | Action.START | false       | false     | Cause.STARTED | State.STARTING | State.INITIAL | Cause.DEPENDENCY_FAILED
//        State.STOPPED | Action.START | false       | false     | Cause.STARTED | State.STARTING | State.STOPPED | Cause.DEPENDENCY_FAILED

    }

    def "stop"(Service.State initialState, RelatedServiceExecutor.Action action, boolean serviceFail, boolean allDepsOk, Service.Cause callWithCause, Service.State transientState, Service.State finalState, Service.Cause finalCause) {

        given:

        service.state = initialState
        service.throwStopException serviceFail

        when:

        ServiceStatus status = service.stop(callWithCause)

        then:
        1 * messageBus.publishSync({ ServiceBusMessage m -> m.toState == transientState && m.cause == callWithCause })
        1 * servicesExecutor.execute(action, callWithCause) >> true
        1 * messageBus.publishSync({ ServiceBusMessage m -> m.toState == finalState && m.cause == finalCause })
        service.getState() == finalState
        service.getCause() == finalCause
        status.state == finalState
        status.cause == finalCause
        status.service == service


        where:
        initialState          | action                             | serviceFail | allDepsOk | callWithCause                    | transientState         | finalState            | finalCause
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | false       | true      | Service.Cause.STOPPED            | Service.State.STOPPING | Service.State.STOPPED | Service.Cause.STOPPED
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | false       | true      | Service.Cause.FAILED             | Service.State.STOPPING | Service.State.FAILED  | Service.Cause.FAILED
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | false       | true      | Service.Cause.DEPENDENCY_STOPPED | Service.State.STOPPING | Service.State.STOPPED | Service.Cause.DEPENDENCY_STOPPED
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | false       | true      | Service.Cause.DEPENDENCY_FAILED  | Service.State.STOPPING | Service.State.STOPPED | Service.Cause.DEPENDENCY_FAILED
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | true        | true      | Service.Cause.STOPPED            | Service.State.STOPPING | Service.State.FAILED  | Service.Cause.FAILED_TO_STOP
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | true        | true      | Service.Cause.FAILED             | Service.State.STOPPING | Service.State.FAILED  | Service.Cause.FAILED
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | true        | true      | Service.Cause.DEPENDENCY_STOPPED | Service.State.STOPPING | Service.State.FAILED  | Service.Cause.FAILED_TO_STOP
        Service.State.RUNNING | RelatedServiceExecutor.Action.STOP | true        | true      | Service.Cause.DEPENDENCY_FAILED  | Service.State.STOPPING | Service.State.FAILED  | Service.Cause.FAILED_TO_STOP
    }


    def "ignored start calls"(Service.State initialState, RelatedServiceExecutor.Action action) {

        given:

        service.state = initialState
        Service.Cause initialCause = service.getCause()

        when:

        ServiceStatus status = service.start()

        then:
        0 * servicesExecutor.execute(action, Service.Cause.STARTED)
        service.getState() == initialState
        service.getCause() == initialCause
        status.state == initialState
        status.cause == initialCause
        status.service == service


        where:
        initialState           | action
        Service.State.STARTING | RelatedServiceExecutor.Action.START
        Service.State.RUNNING  | RelatedServiceExecutor.Action.START
    }

    def "disallowed start calls throw exception"(Service.State initialState, RelatedServiceExecutor.Action action) {

        given:

        service.state = initialState
        Service.Cause initialCause = service.getCause()

        when:

        ServiceStatus status = service.start()

        then:
        thrown(ServiceStatusException)


        where:
        initialState           | action
        Service.State.STOPPING | RelatedServiceExecutor.Action.START
        Service.State.FAILED   | RelatedServiceExecutor.Action.START
    }


    def "ignored stop calls"(Service.State initialState, RelatedServiceExecutor.Action action, Service.Cause callWithCause) {

        given:

        service.state = initialState
        Service.Cause initialCause = service.getCause()

        when:

        ServiceStatus status = service.stop(callWithCause)

        then:
        0 * servicesExecutor.execute(action, callWithCause)
        service.getState() == initialState
        service.getCause() == initialCause
        status.state == initialState
        status.cause == initialCause
        status.service == service


        where:
        initialState            | action                             | callWithCause
        Service.State.STOPPED   | RelatedServiceExecutor.Action.STOP | Service.Cause.STOPPED
        Service.State.STOPPED   | RelatedServiceExecutor.Action.STOP | Service.Cause.FAILED
        Service.State.STOPPED   | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_STOPPED
        Service.State.STOPPED   | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_FAILED
        Service.State.FAILED    | RelatedServiceExecutor.Action.STOP | Service.Cause.STOPPED
        Service.State.FAILED    | RelatedServiceExecutor.Action.STOP | Service.Cause.FAILED
        Service.State.FAILED    | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_STOPPED
        Service.State.FAILED    | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_FAILED
        Service.State.STOPPING  | RelatedServiceExecutor.Action.STOP | Service.Cause.STOPPED
        Service.State.STOPPING  | RelatedServiceExecutor.Action.STOP | Service.Cause.FAILED
        Service.State.STOPPING  | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_STOPPED
        Service.State.STOPPING  | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_FAILED
        Service.State.RESETTING | RelatedServiceExecutor.Action.STOP | Service.Cause.STOPPED
        Service.State.RESETTING | RelatedServiceExecutor.Action.STOP | Service.Cause.FAILED
        Service.State.RESETTING | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_STOPPED
        Service.State.RESETTING | RelatedServiceExecutor.Action.STOP | Service.Cause.DEPENDENCY_FAILED
    }

    def "disallowed stop calls throw exception"() {
        //there are none
        expect: true
    }


    def "reset"(Service.State initialState, RelatedServiceExecutor.Action action, boolean serviceFail, Service.State transientState, Service.State finalState, Service.Cause finalCause) {

        given:

        service.state = initialState
        service.throwResetException serviceFail

        when:

        ServiceStatus status = service.reset()

        then:
        1 * messageBus.publishSync({ ServiceBusMessage m -> m.toState == transientState && m.cause == Service.Cause.RESET })
        1 * messageBus.publishSync({ ServiceBusMessage m -> m.toState == finalState && m.cause == finalCause })
        service.getState() == finalState
        service.getCause() == finalCause
        status.state == finalState
        status.cause == finalCause
        status.service == service


        where:
        initialState          | action                             | serviceFail | transientState          | finalState            | finalCause
        Service.State.STOPPED | RelatedServiceExecutor.Action.STOP | false       | Service.State.RESETTING | Service.State.INITIAL | Service.Cause.RESET
        Service.State.FAILED  | RelatedServiceExecutor.Action.STOP | false       | Service.State.RESETTING | Service.State.INITIAL | Service.Cause.RESET
        Service.State.STOPPED | RelatedServiceExecutor.Action.STOP | true        | Service.State.RESETTING | Service.State.FAILED  | Service.Cause.FAILED_TO_RESET
        Service.State.FAILED  | RelatedServiceExecutor.Action.STOP | true        | Service.State.RESETTING | Service.State.FAILED  | Service.Cause.FAILED_TO_RESET

    }

    def "ignored reset calls"(Service.State initialState) {

        given:

        service.state = initialState
        Service.Cause initialCause = service.getCause()

        when:

        ServiceStatus status = service.reset()

        then:
        service.getState() == initialState
        service.getCause() == initialCause
        status.state == initialState
        status.cause == initialCause
        status.service == service


        where:
        initialState            | _
        Service.State.INITIAL   | _
        Service.State.RESETTING | _
    }

    def "disallowed reset calls throw exception"(Service.State initialState) {

        given:

        service.state = initialState
        Service.Cause initialCause = service.getCause()

        when:

        ServiceStatus status = service.reset()

        then:
        thrown(ServiceStatusException)


        where:
        initialState           | _
        Service.State.STOPPING | _
        Service.State.RUNNING  | _
        Service.State.STARTING | _
    }

    def "fail short call"() {
        given:

        service.state = Service.State.RUNNING

        when:

        service.fail()

        then:

        1 * servicesExecutor.execute(RelatedServiceExecutor.Action.STOP, Service.Cause.FAILED) >> true
    }

    def "dependencyFail"() {

        given:

        service.state = Service.State.RUNNING

        when:

        service.dependencyFail()

        then:

        1 * servicesExecutor.execute(RelatedServiceExecutor.Action.STOP, Service.Cause.DEPENDENCY_FAILED) >> true
    }

    def "dependencyStop"() {

        given:

        service.state = Service.State.RUNNING

        when:

        service.dependencyStop()

        then:

        1 * servicesExecutor.execute(RelatedServiceExecutor.Action.STOP, Service.Cause.DEPENDENCY_STOPPED) >> true

    }

    def "start short call"() {


        when:

        service.start()

        then:

        1 * servicesExecutor.execute(RelatedServiceExecutor.Action.START, Service.Cause.STARTED) >> true

    }

    def "stop"() {

        given:

        service.state = Service.State.RUNNING

        when:

        service.stop()

        then:

        1 * servicesExecutor.execute(RelatedServiceExecutor.Action.STOP, Service.Cause.STOPPED) >> true

    }


    static class TestService extends AbstractService implements Service {

        boolean throwStartException = false
        boolean throwStopException = false
        boolean throwResetException = false

        @Inject
        protected TestService(Translate translate, MessageBus messageBus, RelatedServiceExecutor servicesExecutor, SerializationSupport serializationSupport) {
            super(translate, messageBus, servicesExecutor, serializationSupport)
        }

        @Override
        void doStart() {
            if (throwStartException) {
                throw new RuntimeException("Test Exception thrown")
            }
        }

        @Override
        void doStop() {
            if (throwStopException) {
                throw new RuntimeException("Test Exception thrown")
            }
        }

        @Override
        void doReset() {
            if (throwResetException) {
                throw new RuntimeException("Test Exception thrown")
            }
        }

        @Override
        I18NKey getNameKey() {
            return LabelKey.Authorisation
        }

        void throwStartException(boolean throwStartException) {
            this.throwStartException = throwStartException
        }

        void throwStopException(boolean throwStopException) {
            this.throwStopException = throwStopException
        }

        void throwResetException(boolean throwResetException) {
            this.throwResetException = throwResetException
        }
    }


}