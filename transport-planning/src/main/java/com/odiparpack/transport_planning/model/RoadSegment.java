package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class RoadSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private City origin;

    @ManyToOne
    private City destination;

    private double distance;
    private double speedLimit;
    private double cost; // Costo del segmento en tiempo o valor monetario

    @ElementCollection(fetch = FetchType.EAGER) // Change fetch type to EAGER
    @CollectionTable(name = "blockage_periods", joinColumns = @JoinColumn(name = "road_segment_id"))
    private List<BlockagePeriod> blockagePeriods = new ArrayList<>();

    public RoadSegment() {}

    public void addBlockagePeriod(Date startDate, Date endDate) {
        BlockagePeriod blockage = new BlockagePeriod(startDate, endDate);
        blockagePeriods.add(blockage);
    }

    public boolean isAvailableAt(Date currentSimulationTime) {
        for (BlockagePeriod blockage : blockagePeriods) {
            if (blockage.isWithinBlockage(currentSimulationTime)) {
                return false;
            }
        }
        return true;
    }

    @Embeddable
    @Data
    public static class BlockagePeriod {

        @Temporal(TemporalType.TIMESTAMP)
        private Date startDate;

        @Temporal(TemporalType.TIMESTAMP)
        private Date endDate;

        public BlockagePeriod() {}

        public BlockagePeriod(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public boolean isWithinBlockage(Date time) {
            return (time.after(startDate) || time.equals(startDate)) && time.before(endDate);
        }
    }
}
