package de.atm_search.cseh_17.geld_kompass.ModelOSM;

public class Members
{
    private String ref;

    private String role;

    private String type;

    public String getRef ()
    {
        return ref;
    }

    public void setRef (String ref)
    {
        this.ref = ref;
    }

    public String getRole ()
    {
        return role;
    }

    public void setRole (String role)
    {
        this.role = role;
    }

    public String getType ()
    {
        return type;
    }

    public void setType (String type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [ref = "+ref+", role = "+role+", type = "+type+"]";
    }
}