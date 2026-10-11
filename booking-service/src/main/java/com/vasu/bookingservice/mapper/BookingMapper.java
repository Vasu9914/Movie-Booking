package com.vasu.bookingservice.mapper;

import com.vasu.bookingservice.dto.response.BookingResponse;
import com.vasu.bookingservice.dto.response.BookingSeatResponse;
import com.vasu.bookingservice.dto.response.SeatResponse;
import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.entity.BookingSeat;
import com.vasu.bookingservice.entity.ShowSeat;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    SeatResponse toSeatResponse(ShowSeat showSeat);

    List<SeatResponse> toSeatResponseList(List<ShowSeat> showSeats);

    BookingResponse toBookingResponse(Booking booking);

    BookingSeatResponse toBookingSeatResponse(BookingSeat bookingSeat);

    List<BookingSeatResponse> toBookingSeatResponseList(List<BookingSeat> bookingSeats);
}
