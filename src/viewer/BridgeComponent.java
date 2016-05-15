package viewer;

import javafx.scene.Parent;

public abstract class BridgeComponent {
    
    private Parent root;
    private Bridge bridgeReference;
    
    public void setViewRoot(Parent viewRoot){
        root = viewRoot;
    }

    public Parent getViewRoot(){
        return root;
    }   

    public void setBridgeReference(Bridge bridgeReference){
        this.bridgeReference = bridgeReference;
    }

    public Bridge getBridgeReference(){
        return bridgeReference;
    }  
    
    public abstract void ready();
}
