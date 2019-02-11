package de.atm_search.cseh_17.geld_kompass.ModelOSM;

public class MyOsmAtms
{
    private String generator;

    private Osm3s osm3s;

    private Elements[] elements;

    private String version;

    public String getGenerator ()
    {
        return generator;
    }

    public void setGenerator (String generator)
    {
        this.generator = generator;
    }

    public Osm3s getOsm3s ()
    {
        return osm3s;
    }

    public void setOsm3s (Osm3s osm3s)
    {
        this.osm3s = osm3s;
    }

    public Elements[] getElements ()
    {
        return elements;
    }

    public void setElements (Elements[] elements)
    {
        this.elements = elements;
    }

    public String getVersion ()
    {
        return version;
    }

    public void setVersion (String version)
    {
        this.version = version;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [generator = "+generator+", osm3s = "+osm3s+", elements = "+elements+", version = "+version+"]";
    }
}
