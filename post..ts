import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ExampleService } from './example.service';

describe('ExampleService', () => {
  let service: ExampleService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExampleService],
    });

    service = TestBed.inject(ExampleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // Ensure no unmatched requests are pending
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should make a POST request and return data', () => {
    const dummyData = { success: true };
    const postData = { name: 'John Doe', email: 'john.doe@example.com' };

    service.postData(postData).subscribe((response) => {
      expect(response).toEqual(dummyData);
    });

    const req = httpMock.expectOne('https://example.com/api/data');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('Content-Type')).toBe('application/json');

    req.flush(dummyData); // Provide a mock response
  });

  it('should handle an error response', () => {
    const postData = { name: 'John Doe', email: 'john.doe@example.com' };

    service.postData(postData).subscribe(
      () => fail('Should have failed with the 500 error'),
      (error) => {
        expect(error.status).toBe(500);
      }
    );

    const req = httpMock.expectOne('https://example.com/api/data');
    expect(req.request.method).toBe('POST');

    req.flush('Something went wrong', { status: 500, statusText: 'Server Error' });
  });
});
