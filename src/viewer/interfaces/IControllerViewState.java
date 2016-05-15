/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer.interfaces;

import java.io.File;
import javafx.scene.control.Tab;

/**

 @author Ludek
 */
public interface IControllerViewState {

    public void addTabState(Tab tab, File[] filesInDirectory, int fileIndex, int offset);

    public IViewState getState(Tab tab);

    public void removeTabState(Tab tab);

    public void saveState(Tab sourceTab);

    public void loadState(Tab sourceTab);

    public int getNumberOfStates();
}
