package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Connector;

public class HiddenWire implements Wire
{
	private Connector connectorA, connectorB;
	
	@Override
	public void setConnectorA(Connector connectorA)
	{
		this.connectorA = connectorA;
	}
	
	@Override
	public Connector getConnectorA()
	{
		return connectorA;
	}
	
	@Override
	public void setConnectorB(Connector connectorB)
	{
		this.connectorB = connectorB;
	}
	
	@Override
	public Connector getConnectorB()
	{
		return connectorB;
	}
	
	@Override
	public Connector getOtherSide(Connector connector)
	{
		return connector == connectorA ? connectorB : connectorA;
	}
	
	private Cluster cluster;
	
	@Override
	public void setCluster(Cluster cluster)
	{
		this.cluster = cluster;
	}
	
	@Override
	public Cluster getCluster()
	{
		return cluster;
	}
	
	@Override
	public boolean hasCluster()
	{
		return cluster != null;
	}
}
