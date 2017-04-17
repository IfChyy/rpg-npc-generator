package me.kerooker.characterinformation;


import android.content.Context;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import me.kerooker.enums.Priority;

public class Npc implements Serializable {


    private List<Information> information = new ArrayList<>();
    private UUID uuid;
    private String imageBits;

    public Npc(Information... informations) {
        Collections.addAll(information, informations);

        sortInformation();
    }

    private Npc(List<Information> info) {

        this.information = info;
        sortInformation();
    }

    /**
     * Generates a Random NPC with the following information:
     * Name, race, gender, age, sexuality, profession, motivation, personality traits and language
     *
     * @param context The context to get information from
     * @return A new random npc
     */
    private static Npc generateRandomNpc(Context context) {
        Age age = new Age();
        Gender gender = new Gender();
        Race race = new Race(context);
        Language language = new Language(race);
        Name name = new Name(context);
        Motivation motivation = new Motivation(context);
        PersonalityTraits traits = new PersonalityTraits(context);
        Profession profession = new Profession(age, context);
        Sexuality sexuality = new Sexuality();
        return new Npc(name, race, gender, age, profession, sexuality, motivation, traits, language);
    }

    public static Npc fromJson(String json) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Information.class, new InterfaceAdapter<Information>()).create();
        return gson.fromJson(json, Npc.class);
    }

    public boolean hasImage() {
        return imageBits != null;
    }

    public String getImageBits() {
        return imageBits;
    }

    public void setImageBits(String imageBits) {
        this.imageBits = imageBits;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasUUID() {
        return uuid != null;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setRandomUUID() {
        uuid = UUID.randomUUID();
    }

// --Commented out by Inspection START (12/04/2017 18:41):
//    public void addInformation(Information inf) {
//        information.add(inf);
//    }
// --Commented out by Inspection STOP (12/04/2017 18:41)

    public Information getTopInformation() {
        Information topInformation = null;
        for (Information i : information) {
            if (i.getPriority().equals(Priority.TOP)) {
                topInformation = i;
                break;
            }
        }
        if (topInformation == null) throw new RuntimeException("No top Priority!");
        return topInformation;

    }

    public String toJson() {
        Gson gson = new GsonBuilder().registerTypeAdapter(Information.class, new InterfaceAdapter<Information>()).create();
        return gson.toJson(this);
    }

    public List<Information> getNpcInformation() {
        return information;
    }

    private void sortInformation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            information.sort(getInformationComparator());
        } else {
            //noinspection Java8ListSort
            Collections.sort(information, getInformationComparator());
        }
    }

    public String getCharacter() {

        StringBuilder informationBuilder = new StringBuilder();

        for (Information inf : information) {
            informationBuilder.append(inf.getInformation()).append("\n");
        }
        return informationBuilder.toString();
    }

    private Comparator<Information> getInformationComparator() {
        return (o1, o2) -> {
            Priority o1P = o1.getPriority();
            Priority o2P = o2.getPriority();
            return -(o1P.compareTo(o2P));
        };
    }

    public void setInformation(List<Information> information) {
        this.information.clear();
        this.information.addAll(information);
        sortInformation();
    }

    public static class Builder {
        private Context context;
        private Npc npcInstance;

        public Builder(Context c) {
            this.context = c;
            npcInstance = new Npc();
        }

        public Builder random() {
            npcInstance = generateRandomNpc(context);
            return this;
        }



        public Builder withInformation(Information inf) {
            Iterator<Information> infoIterator = npcInstance.getNpcInformation().iterator();
            boolean shouldAdd = false;
            boolean isRace = false;
            while (infoIterator.hasNext()) {
                Information next = infoIterator.next();
                Class actualClass = next.getClass();
                Class original = inf.getClass();
                if (original.equals(actualClass)) {
                    //Classes are the same, remove from iterator
                    if (original.equals(Race.class)) {
                        //Adding a Race.
                        Race r = (Race) inf;
                        isRace = true;
                    }
                    infoIterator.remove();
                    //Add information to list
                    shouldAdd = true;
                }

            }
            if (shouldAdd) {
                npcInstance.getNpcInformation().add(inf);
                if (isRace) {
                    Race r = (Race) inf;
                    this.withInformation(new Language(r));
                }

            }
            return this;
        }

        public Npc getNpcInstance() {
            npcInstance.sortInformation();
            return npcInstance;
        }
    }

    static final class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
        public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context) {
            final JsonObject wrapper = new JsonObject();
            wrapper.addProperty("type", object.getClass().getName());
            wrapper.add("data", context.serialize(object));
            return wrapper;
        }

        public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
            final JsonObject wrapper = (JsonObject) elem;
            final JsonElement typeName = get(wrapper, "type");
            final JsonElement data = get(wrapper, "data");
            final Type actualType = typeForName(typeName);
            return context.deserialize(data, actualType);
        }

        private Type typeForName(final JsonElement typeElem) {
            try {
                return Class.forName(typeElem.getAsString());
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        private JsonElement get(final JsonObject wrapper, String memberName) {
            final JsonElement elem = wrapper.get(memberName);
            if (elem == null)
                throw new JsonParseException("no '" + memberName + "' member found in what was expected to be an interface wrapper");
            return elem;
        }
    }
}