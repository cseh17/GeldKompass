package com.atm_search.cseh_17.geld_kompass.ModelOSM;

public class Elements
{
    private Tags tags;

    private String id;

    private String lon;

    private String type;

    private String lat;

    public Tags getTags ()
    {
        return tags;
    }

    public void setTags (Tags tags)
    {
        this.tags = tags;
    }

    public String getId ()
    {
        return id;
    }

    public void setId (String id)
    {
        this.id = id;
    }

    public String getLon ()
    {
        return lon;
    }

    public void setLon (String lon)
    {
        this.lon = lon;
    }

    public String getType ()
    {
        return type;
    }

    public void setType (String type)
    {
        this.type = type;
    }

    public String getLat ()
    {
        return lat;
    }

    public void setLat (String lat)
    {
        this.lat = lat;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [tags = "+tags+", id = "+id+", lon = "+lon+", type = "+type+", lat = "+lat+"]";
    }
}
