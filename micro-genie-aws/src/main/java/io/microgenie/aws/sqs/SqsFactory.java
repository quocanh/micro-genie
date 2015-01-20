package io.microgenie.aws.sqs;

import io.microgenie.application.queue.Consumer;
import io.microgenie.application.queue.Message;
import io.microgenie.application.queue.MessageHandler;
import io.microgenie.application.queue.Producer;
import io.microgenie.application.queue.QueueFactory;
import io.microgenie.aws.SqsConfig;
import io.microgenie.aws.SqsConsumerConfig;
import io.microgenie.commands.util.CollectionUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.google.common.collect.Maps;



/**
 * SQS Client Factory
 * @author shawn
 */
public class SqsFactory extends QueueFactory{

	private final AmazonSQSClient sqs;
	private final SqsQueueAdmin admin;
	private final SqsConfig config;
	private final Map<String, Consumer> consumers = Maps.newHashMap();
	
	private Producer producer;
	
	
	/**
	 * @param sqsClient
	 * @param configs
	 * @param tgFactory
	 */
	public SqsFactory(final AmazonSQSClient sqsClient, final SqsConfig config){
		this.sqs = sqsClient;
		this.config = config;
		this.admin = new SqsQueueAdmin(this.sqs);
	}



	@Override
	public Producer getProducer() {
		return this.producer;
	}
	@Override
	public Consumer getConsumer(String queue) {
		return this.consumers.get(queue);
	}
	@Override
	public void submit(final Message message) {
		this.producer.submit(message);
	}
	@Override
	public void submitBatch(List<Message> message) {
		this.producer.submitBatch(message);
	}
	@Override
	public void consume(final String queue, final MessageHandler handler) {
		final Consumer consumer = this.consumers.get(queue);
		consumer.start(handler);
	}
	
	
	/***
	 * Initialize  the following:
	 * <li>SQS Queues</li>
	 * <li>SQS Producer</li>
	 * <li>SQS Consumers</li>
	 */
	@Override
	public void initialize(){
		
		try{
			if(this.config !=null){
				/** Ensure that queues are all created, and if specified, block until all queues are ready **/
				this.admin.initializeQueues(this.config.getQueues(), this.config.isBlockUntilQueueIsReady());
				
				/** Should we create a producer? **/
				if(config.isProduces()){
					this.producer = new SqsProducer(sqs);
				}
				/** Create and initialize any queue consumers **/
				this.initializeConsumers(config.getConsumers());
			}
		}catch(Exception ex){
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
	
	
	/***
	 * Stop all consumers and close the SQS Client
	 */
	@Override
	public void close(){
		this.admin.shutdown();
		if(this.consumers!=null && this.consumers.size()>0){
			for(Entry<String, Consumer> c : consumers.entrySet()){
				c.getValue().stop();
			}
		}
		this.sqs.shutdown();
	}
	
	
	
	
	/***
	 * Initialize Queue Consumers, this also starts the consumers
	 * @param consumers
	 * @throws ClassNotFoundException 
	 */
	private void initializeConsumers(final List<SqsConsumerConfig> consumers) throws ClassNotFoundException {
		if(CollectionUtil.hasElements(consumers)){
			for(SqsConsumerConfig config: consumers){
				config.createHandler();
				final Consumer consumer = new SqsConsumer(this.sqs, config);
				consumer.start(config.getHandlerInstance());
				this.consumers.put(config.getQueue(), consumer);		
			}
		}
	}
}