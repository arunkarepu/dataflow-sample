package com.springml.sample_pubsub_gcs;

import java.io.IOException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;

/**
 * mvn compile exec:java
 * -Dexec.mainClass=com.springml.sample_pubsub_gcs.App
 * -Dexec.args=" --project=alert-height-284720
 * --stagingLocation=gs://springml_test/dataflow/staging
 * --tempLocation=gs://springml_test/dataflow/temp --runner=DataflowRunner
 * --region=us-central1
 * --inputTopic=projects/alert-height-284720/subscriptions/throughput_check_sub
 * 
 */
public class App {
  /*
   * Define your own configuration options. Add your own arguments to be processed
   * by the command-line parser, and specify default values for them.
   */
  public interface PubSubToGcsOptions extends PipelineOptions, StreamingOptions {
    @Description("The Cloud Pub/Sub topic to read from.")
    @Required
    String getInputTopic();

    void setInputTopic(String value);

    @Description("Output file's window size in number of minutes.")
    @Default.Integer(1)
    Integer getWindowSize();

    void setWindowSize(Integer value);
  }

  public static void main(String[] args) throws IOException {
    // The maximum number of shards when writing output.
    int numShards = 1;

    PubSubToGcsOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(PubSubToGcsOptions.class);

    options.setStreaming(true);

    Pipeline pipeline = Pipeline.create(options);

    pipeline
        // 1) Read string messages from a Pub/Sub topic.
        .apply("Read PubSub Messages", PubsubIO.readStrings().fromTopic(options.getInputTopic()))
        // 2) Group the messages into fixed-sized minute intervals.
        .apply(Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowSize()))))
        // 3) Write one file to GCS for every window of messages.
        .apply("Write Data to Output", ParDo.of(new TransformData()));

    // Execute the pipeline and wait until it finishes running.
    pipeline.run();
  }
  
  public static class TransformData extends DoFn<String, String> {
	private static final long serialVersionUID = 1L;
	
	@ProcessElement
	public void processElement(@Element String input, OutputReceiver<String> out, ProcessContext c) {
		System.out.println(input);
		out.output(input);
	}
  }
  
}