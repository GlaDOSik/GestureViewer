
package viewer.VC;

import java.io.File;
import viewer.interfaces.IViewState;

public class VariantState implements IViewState{

    public File[] filesInDirectory;
    public int offset;
    public int fileIndex;
    public int currentPage = 0;
    public double vValue = 0;
    
    private boolean isActive = false; 
    
    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setActiveState(boolean state) {
        isActive = state;
    }
    
}
