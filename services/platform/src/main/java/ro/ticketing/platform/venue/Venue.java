package ro.ticketing.platform.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

@Entity
@Table(name = "venues")
class Venue extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 300)
    private String address;

    protected Venue() {
    }

    Venue(String name, String city, String address) {
        this.name = name;
        this.city = city;
        this.address = address;
    }

    String getName() {
        return name;
    }

    String getCity() {
        return city;
    }

    String getAddress() {
        return address;
    }
}
