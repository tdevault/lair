package core;

/**
 *
 * @author zoran
 */
public interface NeuralNetworkEventListener extends  java.util.EventListener {
    
      public void handleNeuralNetworkEvent(NeuralNetworkEvent event);
}
