package org.strategoxt.imp.runtime.services.menus.builders;

import java.util.LinkedList;
import java.util.List;

public class Menu implements IMenuContribution {

	private final String caption;
	private final List<IMenuContribution> menuContribs = new LinkedList<IMenuContribution>();
	
	public Menu(String caption) {
		this.caption = caption;
	}
	
	@Override
	public String getCaption() {
		return caption;
	}

	public void addMenuContribution(IMenuContribution menuContrib) {
		menuContribs.add(menuContrib);
	}
	
	public List<IMenuContribution> getMenuContributions() {
		return menuContribs;
	}
}
