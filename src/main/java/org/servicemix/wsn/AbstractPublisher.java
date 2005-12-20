package org.servicemix.wsn;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.oasis_open.docs.wsn.b_1.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.TopicExpressionType;
import org.oasis_open.docs.wsn.br_1.Destroy;
import org.oasis_open.docs.wsn.br_1.DestroyResponse;
import org.oasis_open.docs.wsn.br_1.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_1.RegisterPublisher;
import org.oasis_open.docs.wsn.br_1.ResourceNotDestroyedFaultType;
import org.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.servicemix.wsn.jaxws.PublisherRegistrationFailedFault;
import org.servicemix.wsn.jaxws.PublisherRegistrationManager;
import org.servicemix.wsn.jaxws.PublisherRegistrationRejectedFault;
import org.servicemix.wsn.jaxws.ResourceNotDestroyedFault;
import org.servicemix.wsn.jaxws.ResourceUnknownFault;
import org.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.w3._2005._03.addressing.EndpointReferenceType;

@WebService(endpointInterface = "org.servicemix.wsn.jaxws.PublisherRegistrationManager")
public abstract class AbstractPublisher extends AbstractEndpoint 
									    implements PublisherRegistrationManager {

	protected EndpointReferenceType publisherReference;
	protected boolean demand;
	protected List<TopicExpressionType> topic;
	
	public AbstractPublisher(String name) {
		super(name);
	}
	
    /**
     * 
     * @param destroyRequest
     * @return
     *     returns org.oasis_open.docs.wsn.br_1.DestroyResponse
     * @throws ResourceNotDestroyedFault
     * @throws ResourceUnknownFault
     */
    @WebMethod(operationName = "Destroy")
    @WebResult(name = "DestroyResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-1", partName = "DestroyResponse")
    public DestroyResponse destroy(
        @WebParam(name = "Destroy", targetNamespace = "http://docs.oasis-open.org/wsn/br-1", partName = "DestroyRequest")
        Destroy destroyRequest)
        throws ResourceNotDestroyedFault, ResourceUnknownFault {
    	
    	destroy();
    	return new DestroyResponse();
    }
    
    public abstract void notify(NotificationMessageHolderType messageHolder);

    protected void destroy() throws ResourceNotDestroyedFault {
    	try {
    		unregister();
    	} catch (EndpointRegistrationException e) {
    		ResourceNotDestroyedFaultType fault = new ResourceNotDestroyedFaultType();
    		throw new ResourceNotDestroyedFault("Error unregistering endpoint", fault, e);
    	}
    }

	protected String createAddress() {
		return "http://servicemix.org/wsnotification/Publisher/" + getName();
	}

	public void create(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault, TopicNotSupportedFault {
		validatePublisher(registerPublisherRequest);
		start();
	}
	
	protected void validatePublisher(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault, TopicNotSupportedFault {
		// Check consumer reference
		publisherReference = registerPublisherRequest.getPublisherReference();
		// Check topic
		topic = registerPublisherRequest.getTopic();
		// Check demand based
		demand = registerPublisherRequest.isDemand() != null ? registerPublisherRequest.isDemand().booleanValue() : false;
		// Check all parameters
		if (publisherReference == null) {
			PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
			throw new PublisherRegistrationFailedFault("Invalid PublisherReference: null", fault);
		}
		if (demand) {
			if (topic == null || topic.size() == 0) {
				InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
				throw new InvalidTopicExpressionFault("Must specify at least one topic for demand-based publishing", fault);
			}
		}
	}
	
	protected abstract void start() throws PublisherRegistrationFailedFault;
}
