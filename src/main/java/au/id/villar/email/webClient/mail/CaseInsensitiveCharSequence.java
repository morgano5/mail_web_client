package au.id.villar.email.webClient.mail;

@Deprecated
class CaseInsensitiveCharSequence implements CharSequence {
    private final String name;

    public CaseInsensitiveCharSequence(CharSequence name) {
        this.name = name.toString().toLowerCase();
    }

    @Override
    public int length() {
        return name.length();
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new CaseInsensitiveCharSequence(name.subSequence(start, end));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !CharSequence.class.isAssignableFrom(o.getClass())) return false;
        CharSequence charSequence = (CharSequence)o;
        if (charSequence.length() != this.length()) return false;
        for(int i = 0; i < charSequence.length(); i++) {
            char ch = Character.toLowerCase(charSequence.charAt(i));
            if(ch != name.charAt(i)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
