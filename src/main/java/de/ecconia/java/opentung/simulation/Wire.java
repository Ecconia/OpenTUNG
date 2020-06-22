package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Connector;

public interface Wire extends Clusterable
{
	Connector getOtherSide(Connector connector);
	
	void setConnectorA(Connector connectorA);
	
	void setConnectorB(Connector connectorB);
	
	Connector getConnectorA();
	
	Connector getConnectorB();
}
